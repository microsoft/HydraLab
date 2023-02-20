// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import com.microsoft.hydralab.agent.runner.ITestRun;
import com.microsoft.hydralab.agent.runner.TestRunThreadContext;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.performance.inspectors.AndroidBatteryInfoInspector;
import com.microsoft.hydralab.performance.inspectors.WindowsBatteryInspector;
import com.microsoft.hydralab.performance.inspectors.WindowsMemoryInspector;
import com.microsoft.hydralab.performance.parsers.AndroidBatteryInfoResultParser;
import com.microsoft.hydralab.performance.parsers.WindowsBatteryResultParser;
import com.microsoft.hydralab.performance.parsers.WindowsMemoryResultParser;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
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

import static com.microsoft.hydralab.performance.PerformanceInspector.PerformanceInspectorType.*;
import static com.microsoft.hydralab.performance.PerformanceResultParser.PerformanceResultParserType.*;

@Service
public class PerformanceTestManagementService implements IPerformanceInspectionService, PerformanceTestListener {
    static final ScheduledExecutorService timerExecutor = Executors.newScheduledThreadPool(5 /* corePoolSize */);
    private static final Map<PerformanceInspector.PerformanceInspectorType, PerformanceResultParser.PerformanceResultParserType> inspectorParserTypeMap = Map.of(
            INSPECTOR_ANDROID_BATTERY_INFO, PARSER_ANDROID_BATTERY_INFO,
            INSPECTOR_WIN_MEMORY, PARSER_WIN_MEMORY,
            INSPECTOR_WIN_BATTERY, PARSER_WIN_BATTERY
    );
    private final Map<PerformanceInspector.PerformanceInspectorType, PerformanceInspector> performanceInspectorMap = Map.of(
            INSPECTOR_ANDROID_BATTERY_INFO, new AndroidBatteryInfoInspector(),
            INSPECTOR_WIN_MEMORY, new WindowsMemoryInspector(),
            INSPECTOR_WIN_BATTERY, new WindowsBatteryInspector()
    );
    private final Map<PerformanceResultParser.PerformanceResultParserType, PerformanceResultParser> performanceResultParserMap = Map.of(
            PARSER_ANDROID_BATTERY_INFO, new AndroidBatteryInfoResultParser(),
            PARSER_WIN_MEMORY, new WindowsMemoryResultParser(),
            PARSER_WIN_BATTERY, new WindowsBatteryResultParser()
    );

    private final Map<String, List<ScheduledFuture<?>>> inspectPerformanceTimerMap = new ConcurrentHashMap<>();
    private final Map<String, List<InspectionStrategy>> testLifeCycleStrategyMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, PerformanceTestResult>> testRunPerfResultMap = new ConcurrentHashMap<>();

    public PerformanceTestManagementService() {
        PerformanceInspectionService.getInstance().swapImplementation(this);
    }

    @NotNull
    private static PerformanceTestResult createPerformanceTestResult(PerformanceInspection performanceInspection) {
        PerformanceTestResult performanceTestResult = new PerformanceTestResult();
        performanceTestResult.inspectorType = performanceInspection.inspectorType;
        performanceTestResult.parserType = getParserTypeByInspection(performanceInspection);
        return performanceTestResult;
    }

    private static PerformanceResultParser.PerformanceResultParserType getParserTypeByInspection(PerformanceInspection performanceInspection) {
        return inspectorParserTypeMap.get(performanceInspection.inspectorType);
    }

    private PerformanceInspector getInspectorByType(PerformanceInspector.PerformanceInspectorType inspectorType) {
        return performanceInspectorMap.get(inspectorType);
    }

    private PerformanceResultParser getParserByType(PerformanceResultParser.PerformanceResultParserType parserType) {
        return performanceResultParserMap.get(parserType);
    }

    @Override
    public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
        performanceInspection = getDevicePerformanceInspection(performanceInspection);
        PerformanceInspector.PerformanceInspectorType inspectorType = performanceInspection.inspectorType;
        PerformanceInspector performanceInspector = getInspectorByType(inspectorType);
        Assert.notNull(performanceInspector, "Found no matched inspector: " + performanceInspection.inspectorType);
        ITestRun testRun = getTestRun();
        File performanceFolder = new File(testRun.getResultFolder(), PerformanceInspection.class.getName());
        Assert.isTrue(performanceFolder.exists() || performanceFolder.mkdirs(), "performanceInspection.resultFolder.mkdirs() failed in " + performanceFolder.getAbsolutePath());
        File inspectorFolder = new File(performanceFolder, inspectorType.name());
        Assert.isTrue(inspectorFolder.exists() || inspectorFolder.mkdirs(), "performanceInspection.resultFolder.mkdirs() failed in " + inspectorFolder.getAbsolutePath());
        performanceInspection.resultFolder = inspectorFolder;

        PerformanceInspectionResult result = performanceInspector.inspect(performanceInspection);

        testRunPerfResultMap.putIfAbsent(getTestRun().getId(), new HashMap<>());
        Map<String, PerformanceTestResult> performanceTestResultMap = testRunPerfResultMap.get(getTestRun().getId());
        Assert.notNull(performanceTestResultMap, "performanceTestResultMap should not be null ");
        performanceTestResultMap.putIfAbsent(performanceInspection.inspectionKey, createPerformanceTestResult(performanceInspection));
        PerformanceTestResult performanceTestResult = performanceTestResultMap.get(performanceInspection.inspectionKey);
        Assert.notNull(performanceTestResult, "performanceTestResult should not be null ");
        performanceTestResult.performanceInspectionResults.add(result);

        return result;
    }

    /**
     * @return the test run object from TestRunThreadContext
     */
    private ITestRun getTestRun() {
        return TestRunThreadContext.getTestRun();
    }

    public void inspectWithStrategy(InspectionStrategy inspectionStrategy) {
        if (inspectionStrategy == null || inspectionStrategy.inspection == null) return;

        if (inspectionStrategy.strategyType == InspectionStrategy.StrategyType.TEST_SCHEDULE) {
            PerformanceInspection inspection = inspectionStrategy.inspection;
            /* initialize inspector */
            PerformanceInspection initialInspection = new PerformanceInspection(InspectionStrategy.WhenType.TEST_RUN_STARTED.name(),
                    inspection.inspectorType, inspection.appId, inspection.deviceIdentifier, true);
            inspect(initialInspection);

            ScheduledFuture<?> scheduledFuture = timerExecutor.scheduleAtFixedRate(() -> {
                inspect(inspection);
            }, 0, inspectionStrategy.interval, inspectionStrategy.intervalUnit);
            inspectPerformanceTimerMap.putIfAbsent(getTestRun().getId(), new ArrayList<>());
            inspectPerformanceTimerMap.get(getTestRun().getId()).add(scheduledFuture);
        }
        if (inspectionStrategy.strategyType == InspectionStrategy.StrategyType.TEST_LIFECYCLE) {
            testLifeCycleStrategyMap.putIfAbsent(getTestRun().getId(), new ArrayList<>());
            testLifeCycleStrategyMap.get(getTestRun().getId()).add(inspectionStrategy);
        }
    }

    @Override
    public PerformanceTestResult parse(PerformanceInspection performanceInspection) {
        performanceInspection = getDevicePerformanceInspection(performanceInspection);
        Map<String, PerformanceTestResult> testResultMap = testRunPerfResultMap.get(getTestRun().getId());
        Assert.notNull(testResultMap, "Found no matched test result for test run");
        PerformanceTestResult performanceTestResult = testResultMap.get(performanceInspection.inspectionKey);
        Assert.notNull(performanceTestResult, "Found no matched performanceTestResult for performanceInspectionKey: " + performanceInspection.inspectionKey);
        PerformanceResultParser parser = getParserByType(performanceTestResult.parserType);
        Assert.notNull(parser, "Found no matched result parser: " + performanceTestResult.parserType);
        return parser.parse(performanceTestResult);
    }

    @Override
    public void testRunStarted() {
        inspectWithLifeCycle(InspectionStrategy.WhenType.TEST_RUN_STARTED, "");
    }

    @Override
    public void testRunFinished() {
        inspectWithLifeCycle(InspectionStrategy.WhenType.TEST_RUN_FINISHED, "");
    }

    @Override
    public void testStarted(String testName) {
        inspectWithLifeCycle(InspectionStrategy.WhenType.TEST_STARTED, testName);
    }

    @Override
    public void testSuccess(String testName) {
        inspectWithLifeCycle(InspectionStrategy.WhenType.TEST_SUCCESS, testName);
    }

    @Override
    public void testFailure(String testName) {
        inspectWithLifeCycle(InspectionStrategy.WhenType.TEST_FAILURE, testName);
    }

    public void testTearDown(DeviceInfo deviceInfo) {
        ITestRun testRun = getTestRun();

        List<PerformanceTestResult> resultList = parseForTestRun(testRun);
        savePerfTestResults(resultList);

        List<ScheduledFuture<?>> timerList = inspectPerformanceTimerMap.get(testRun.getId());
        if (timerList != null) {
            for (ScheduledFuture<?> timer : timerList) {
                timer.cancel(true);
            }
        }

        inspectPerformanceTimerMap.remove(testRun.getId());
        testLifeCycleStrategyMap.remove(testRun.getId());
        testRunPerfResultMap.remove(testRun.getId());

        //TODO Android battery: adb shell dumpsys battery reset using Device info
    }

    private void inspectWithLifeCycle(InspectionStrategy.WhenType whenType, String description) {
        List<InspectionStrategy> strategyList = testLifeCycleStrategyMap.get(getTestRun().getId());
        if (strategyList == null) return;

        for (InspectionStrategy inspectionStrategy : strategyList) {
            if (inspectionStrategy == null) continue;
            PerformanceInspection inspection = inspectionStrategy.inspection;
            if (inspectionStrategy.when == null) continue;

            if (whenType == InspectionStrategy.WhenType.TEST_RUN_STARTED) {
                // initialize inspector
                PerformanceInspection initialInspection = new PerformanceInspection(
                        InspectionStrategy.WhenType.TEST_RUN_STARTED.name(),
                        inspection.inspectorType, inspection.appId, inspection.deviceIdentifier, true);
                inspect(initialInspection);
            } else if (inspectionStrategy.when.contains(whenType)) {
                PerformanceInspection lifeCycleInspection = new PerformanceInspection(
                        whenType.name() + "-" + description,
                        inspection.inspectorType, inspection.appId, inspection.deviceIdentifier, false);
                inspect(lifeCycleInspection);
            }
        }
    }

    /**
     * For giving inspection return the inspection with device id that related to test run
     */
    private PerformanceInspection getDevicePerformanceInspection(PerformanceInspection inspection) {
        if (getTestRun() instanceof TestRun) {
            TestRun testRun = (TestRun) getTestRun();
            return new PerformanceInspection(inspection.description, inspection.inspectorType, inspection.appId,
                    // For windows inspector, the deviceIdentifier is useless
                    testRun.getDeviceSerialNumber(), inspection.isReset);
        }
        return inspection;
    }

    private List<PerformanceTestResult> parseForTestRun(ITestRun testRun) {
        Map<String, PerformanceTestResult> testResultMap = testRunPerfResultMap.get(testRun.getId());
        if (testResultMap == null) return null;

        List<PerformanceTestResult> resultList = new ArrayList<>();
        for (PerformanceTestResult performanceTestResult : testResultMap.values()) {
            PerformanceResultParser parser = getParserByType(performanceTestResult.parserType);
            Assert.notNull(parser, "Found no matched result parser: " + performanceTestResult.parserType);
            resultList.add(parser.parse(performanceTestResult));
        }
        return resultList;
    }

    private void savePerfTestResults(List<PerformanceTestResult> resultList) {
        //TODO save results to DB
        if (resultList != null && !resultList.isEmpty()) {
            FileUtil.writeToFile(resultList.toString(),
                    new File(getTestRun().getResultFolder(), "performance") + File.separator + "PerformanceReport.txt");
        }
    }
}
