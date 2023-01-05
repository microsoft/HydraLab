package com.microsoft.hydralab.performance;

import com.microsoft.hydralab.ITestRun;
import com.microsoft.hydralab.TestRunThreadContext;
import com.microsoft.hydralab.performance.impl.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class PerformanceTestManagementService implements IPerformanceInspectionService {
    static final ScheduledExecutorService timerExecutor = Executors.newScheduledThreadPool(5 /* corePoolSize */);
    private final Map<String, PerformanceInspector> performanceInspectorMap = Map.of(
            PerformanceInspection.INSPECTOR_ANDROID_BATTERY_INFO, new AndroidBatteryInspector(),
            PerformanceInspection.INSPECTOR_ANDROID_MEMORY_DUMP, new AndroidMemoryDumpInspector(),
            PerformanceInspection.INSPECTOR_ANDROID_MEMORY_INFO, new AndroidMemoryInfoInspector(),
            PerformanceInspection.INSPECTOR_WIN_BATTERY, new WindowsBatteryInspector(),
            PerformanceInspection.INSPECTOR_WIN_MEMORY, new WindowsMemoryInspector()
    );
    private final Map<ITestRun, List<ScheduledFuture<?>>> inspectPerformanceTimerMap = new ConcurrentHashMap<>();
    private final Map<ITestRun, Map<String, PerformanceTestResult>> testRunPerfResultMap = new ConcurrentHashMap<>();

    public void initialize() {
        PerformanceInspectionService.getInstance().swapServiceInstance(this);
    }

    /**
     * Maybe we can merge this to inspect method? add a boolean param called reset.
     */
    public void reset(PerformanceInspection performanceInspection) {
        PerformanceInspector performanceInspector = getInspectorByName(performanceInspection.inspector);
        if (performanceInspector == null) {
            return;
        }
        performanceInspector.initialize(performanceInspection);
    }

    private PerformanceInspector getInspectorByName(String inspectorName) {
        return performanceInspectorMap.get(inspectorName);
    }

    public void inspectWithStrategy(PerformanceInspection performanceInspection, InspectionStrategy inspectionStrategy) {
        ScheduledFuture<?> scheduledFuture = timerExecutor.schedule(() -> {
            inspect(performanceInspection);
        }, inspectionStrategy.interval, inspectionStrategy.intervalUnit);
        inspectPerformanceTimerMap.putIfAbsent(getTestRun(), new ArrayList<>());
        inspectPerformanceTimerMap.get(getTestRun()).add(scheduledFuture);
    }

    public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
        String inspector = performanceInspection.inspector;
        PerformanceInspector performanceInspector = getInspectorByName(inspector);
        Assert.notNull(performanceInspector, "Found no matched inspector: " + performanceInspection.inspector);
        ITestRun testRun = getTestRun();
        performanceInspection.resultFolder = new File(testRun.getTestRunResultFolder(), "performance");
        Assert.isTrue(performanceInspection.resultFolder.mkdirs(), "performanceInspection.resultFolder.mkdirs() failed in " + performanceInspection.resultFolder.getAbsolutePath());

        PerformanceInspectionResult result = performanceInspector.inspect(performanceInspection);

        Map<String, PerformanceTestResult> performanceTestResultMap = testRunPerfResultMap.putIfAbsent(getTestRun(), new HashMap<>());
        Assert.notNull(performanceTestResultMap, "performanceTestResultMap should not be null ");
        PerformanceTestResult performanceTestResult = performanceTestResultMap.putIfAbsent(performanceInspection.inspectionKey, createPerformanceTestResult(performanceInspection));
        Assert.notNull(performanceTestResult, "performanceTestResult should not be null ");
        performanceTestResult.performanceInspectionResults.add(result);

        return result;
    }

    @NotNull
    private static PerformanceTestResult createPerformanceTestResult(PerformanceInspection performanceInspection) {
        PerformanceTestResult performanceTestResult = new PerformanceTestResult();
        performanceTestResult.inspector = performanceInspection.inspector;
        return performanceTestResult;
    }

    @NotNull
    private static ITestRun getTestRun() {
        ITestRun testRun = TestRunThreadContext.getTestRun();
        Assert.notNull(testRun, "TestRunThreadContext has no TestRun instance");
        return testRun;
    }

    public List<PerformanceTestResult> parse() {
        Map<String, PerformanceTestResult> performanceTestResults = testRunPerfResultMap.remove(getTestRun());
        List<PerformanceTestResult> parsedPerformanceTestResults = new ArrayList<>();
        Assert.notNull(performanceTestResults, "No performance result to parse for current test run");
        for (PerformanceTestResult performanceTestResult : performanceTestResults.values()) {
            PerformanceTestResult result = getInspectorByName(performanceTestResult.inspector).parse(performanceTestResult.performanceInspectionResults);
            parsedPerformanceTestResults.add(result);
        }
        List<ScheduledFuture<?>> scheduledFutures = inspectPerformanceTimerMap.remove(getTestRun());
        for (ScheduledFuture<?> scheduledFuture : scheduledFutures) {
            scheduledFuture.cancel(true);
        }
        return parsedPerformanceTestResults;
    }
}
