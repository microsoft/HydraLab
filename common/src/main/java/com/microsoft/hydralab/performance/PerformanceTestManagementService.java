// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import com.microsoft.hydralab.agent.runner.ITestRun;
import com.microsoft.hydralab.agent.runner.TestRunThreadContext;
import com.microsoft.hydralab.performance.inspectors.AndroidBatteryInfoInspector;
import com.microsoft.hydralab.performance.inspectors.WindowsMemoryInspector;
import com.microsoft.hydralab.performance.parsers.AndroidBatteryInfoResultParser;
import com.microsoft.hydralab.performance.parsers.WindowsMemoryResultParser;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PerformanceTestManagementService implements IPerformanceInspectionService {
    private final Map<String, PerformanceInspector> performanceInspectorMap = Map.of(
            PerformanceInspector.INSPECTOR_ANDROID_BATTERY_INFO, new AndroidBatteryInfoInspector(),
            PerformanceInspector.INSPECTOR_WIN_MEMORY, new WindowsMemoryInspector()
    );
    private final Map<String, PerformanceResultParser> performanceResultParserMap = Map.of(
            PerformanceResultParser.PARSER_ANDROID_BATTERY_INFO, new AndroidBatteryInfoResultParser(),
            PerformanceResultParser.PARSER_WIN_MEMORY, new WindowsMemoryResultParser()
    );
    private final Map<ITestRun, Map<String, PerformanceTestResult>> testRunPerfResultMap = new ConcurrentHashMap<>();

    @NotNull
    private static PerformanceTestResult createPerformanceTestResult(PerformanceInspection performanceInspection) {
        PerformanceTestResult performanceTestResult = new PerformanceTestResult();
        performanceTestResult.inspectorName = performanceInspection.inspectorName;
        return performanceTestResult;
    }

    public void initialize() {
        PerformanceInspectionService.getInstance().swapImplementation(this);
    }

    private PerformanceInspector getInspectorByName(String inspectorName) {
        return performanceInspectorMap.get(inspectorName);
    }

    private PerformanceResultParser getParserByName(String parserName) {
        return performanceResultParserMap.get(parserName);
    }

    @Override
    public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
        String inspectorName = performanceInspection.inspectorName;
        PerformanceInspector performanceInspector = getInspectorByName(inspectorName);
        Assert.notNull(performanceInspector, "Found no matched inspector: " + performanceInspection.inspectorName);
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

    /**
     * @return the test run object from TestRunThreadContext
     */
    private ITestRun getTestRun() {
        return TestRunThreadContext.getTestRun();
    }

    @Override
    public void inspectWithStrategy(PerformanceInspection performanceInspection, InspectionStrategy inspectionStrategy) {
        //todo
    }

    public PerformanceTestResult parse(PerformanceInspection performanceInspection, String resultParser) {
        Map<String, PerformanceTestResult> testResultMap = testRunPerfResultMap.get(getTestRun());
        Assert.notNull(testResultMap, "Found no matched test result for test run");
        PerformanceTestResult performanceTestResult = testResultMap.get(performanceInspection.inspectionKey);
        Assert.notNull(performanceTestResult, "Found no matched performanceTestResult for performanceInspectionKey: " + performanceInspection.inspectionKey);
        List<PerformanceInspectionResult> performanceInspectionResultList = performanceTestResult.performanceInspectionResults;
        PerformanceResultParser parser = getParserByName(resultParser);
        Assert.notNull(parser, "Found no matched result parser: " + resultParser);
        return parser.parse(performanceInspectionResultList);
    }
}
