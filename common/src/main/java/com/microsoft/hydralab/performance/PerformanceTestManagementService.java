// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import com.microsoft.hydralab.agent.runner.ITestRun;
import com.microsoft.hydralab.agent.runner.TestRunThreadContext;
import com.microsoft.hydralab.performance.inspectors.AndroidBatteryInfoInspector;
import com.microsoft.hydralab.performance.inspectors.WindowsBatteryInspector;
import com.microsoft.hydralab.performance.inspectors.WindowsMemoryInspector;
import com.microsoft.hydralab.performance.parsers.AndroidBatteryInfoResultParser;
import com.microsoft.hydralab.performance.parsers.WindowsBatteryResultParser;
import com.microsoft.hydralab.performance.parsers.WindowsMemoryResultParser;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.microsoft.hydralab.performance.PerformanceInspector.PerformanceInspectorType.*;
import static com.microsoft.hydralab.performance.PerformanceResultParser.PerformanceResultParserType.*;

public class PerformanceTestManagementService implements IPerformanceInspectionService {
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
    private static final Map<PerformanceInspector.PerformanceInspectorType, PerformanceResultParser.PerformanceResultParserType> inspectorParserTypeMap = Map.of(
            INSPECTOR_ANDROID_BATTERY_INFO, PARSER_ANDROID_BATTERY_INFO,
            INSPECTOR_WIN_MEMORY, PARSER_WIN_MEMORY,
            INSPECTOR_WIN_BATTERY, PARSER_WIN_BATTERY
    );
    private final Map<ITestRun, Map<String, PerformanceTestResult>> testRunPerfResultMap = new ConcurrentHashMap<>();

    @NotNull
    private static PerformanceTestResult createPerformanceTestResult(PerformanceInspection performanceInspection) {
        PerformanceTestResult performanceTestResult = new PerformanceTestResult();
        performanceTestResult.inspectorType = performanceInspection.inspectorType;
        performanceTestResult.parserType = getParserTypeByInspection(performanceInspection);
        return performanceTestResult;
    }

    public void initialize() {
        PerformanceInspectionService.getInstance().swapImplementation(this);
    }

    private PerformanceInspector getInspectorByType(PerformanceInspector.PerformanceInspectorType inspectorType) {
        return performanceInspectorMap.get(inspectorType);
    }

    private PerformanceResultParser getParserByType(PerformanceResultParser.PerformanceResultParserType parserType) {
        return performanceResultParserMap.get(parserType);
    }

    private static PerformanceResultParser.PerformanceResultParserType getParserTypeByInspection(PerformanceInspection performanceInspection) {
        return inspectorParserTypeMap.get(performanceInspection.inspectorType);
    }

    @Override
    public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
        PerformanceInspector.PerformanceInspectorType inspectorType = performanceInspection.inspectorType;
        PerformanceInspector performanceInspector = getInspectorByType(inspectorType);
        Assert.notNull(performanceInspector, "Found no matched inspector: " + performanceInspection.inspectorType);
        ITestRun testRun = getTestRun();
        File performanceFolder = new File(testRun.getResultFolder(), "performance");
        Assert.isTrue(performanceFolder.exists() || performanceFolder.mkdirs(), "performanceInspection.resultFolder.mkdirs() failed in " + performanceFolder.getAbsolutePath());
        File inspectorFolder = new File(performanceFolder, inspectorType.getName());
        Assert.isTrue(inspectorFolder.exists() || inspectorFolder.mkdirs(), "performanceInspection.resultFolder.mkdirs() failed in " + inspectorFolder.getAbsolutePath());
        performanceInspection.resultFolder = inspectorFolder;

        PerformanceInspectionResult result = performanceInspector.inspect(performanceInspection);

        testRunPerfResultMap.putIfAbsent(getTestRun(), new HashMap<>());
        Map<String, PerformanceTestResult> performanceTestResultMap = testRunPerfResultMap.get(getTestRun());
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

    @Override
    public void inspectWithStrategy(PerformanceInspection performanceInspection, InspectionStrategy inspectionStrategy) {
        //todo
    }

    @Override
    public PerformanceTestResult parse(PerformanceInspection performanceInspection) {
        Map<String, PerformanceTestResult> testResultMap = testRunPerfResultMap.get(getTestRun());
        Assert.notNull(testResultMap, "Found no matched test result for test run");
        PerformanceTestResult performanceTestResult = testResultMap.get(performanceInspection.inspectionKey);
        Assert.notNull(performanceTestResult, "Found no matched performanceTestResult for performanceInspectionKey: " + performanceInspection.inspectionKey);
        PerformanceResultParser parser = getParserByType(performanceTestResult.parserType);
        Assert.notNull(parser, "Found no matched result parser: " + performanceTestResult.parserType);
        return parser.parse(performanceTestResult);
    }
}
