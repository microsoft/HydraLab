package com.microsoft.hydralab.performance;

import com.microsoft.hydralab.agent.runner.ITestRun;
import com.microsoft.hydralab.agent.runner.TestRunThreadContext;
import com.microsoft.hydralab.performance.inspectors.AndroidBatteryInfoInspector;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PerformanceTestManagementService implements IPerformanceInspectionService {
    private final Map<String, PerformanceInspector> performanceInspectorMap = Map.of(
            PerformanceInspector.INSPECTOR_ANDROID_BATTERY_INFO, new AndroidBatteryInfoInspector()
    );
    private final Map<ITestRun, Map<String, PerformanceTestResult>> testRunPerfResultMap = new ConcurrentHashMap<>();


    public void initialize() {
        PerformanceInspectionService.getInstance().swapImplementation(this);
    }
    private PerformanceInspector getInspectorByName(String inspectorName) {
        return performanceInspectorMap.get(inspectorName);
    }
    @Override
    public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
        String inspector = performanceInspection.inspector;
        PerformanceInspector performanceInspector = getInspectorByName(inspector);
        Assert.notNull(performanceInspector, "Found no matched inspector: " + performanceInspection.inspector);
        ITestRun testRun = getTestRun();
        File performanceFolder = new File(testRun.getResultFolder(), "performance");
        Assert.isTrue(performanceFolder.mkdirs(), "performanceInspection.resultFolder.mkdirs() failed in " + performanceFolder.getAbsolutePath());
        performanceInspection.resultFolder = performanceFolder;

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

    /**
     * @return the test run object from TestRunThreadContext
     */
    private ITestRun getTestRun() {
        return TestRunThreadContext.getTestRun();
    }

    @Override
    public void inspectWithStrategy(PerformanceInspection performanceInspection, InspectionStrategy inspectionStrategy) {

    }
}
