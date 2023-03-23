// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.performance;

import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import com.microsoft.hydralab.performance.entity.AndroidBatteryInfo;
import com.microsoft.hydralab.performance.parsers.AndroidBatteryInfoResultParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author taoran
 * @date 2/22/2023
 */

public class AndroidBatteryInfoResultParserTest {
    public static final String BATTERY_A13_FILE_PATH = "src/test/resources/sample_battery_info_android_13.txt";
    public static final String BATTERY_A10_FILE_PATH = "src/test/resources/sample_battery_info_android_10.txt";


    @Test
    public void testParseWithNull_ReturnNull() {
        AndroidBatteryInfoResultParser parser = new AndroidBatteryInfoResultParser();
        PerformanceTestResult testResult = parser.parse(null);
        Assertions.assertNull(testResult);
    }

    @Test
    public void testParseWithA13_ReturnNull() {
        File batteryFile = new File(BATTERY_A13_FILE_PATH);
        PerformanceTestResult parsedResult = new AndroidBatteryInfoResultParser()
                .parse(createPerformanceTestResultForTest(batteryFile));
        AndroidBatteryInfo summary = (AndroidBatteryInfo) parsedResult.getResultSummary();

        Assertions.assertNotNull(parsedResult);
        Assertions.assertEquals(0.000235f, summary.getAppUsage());
        Assertions.assertEquals(5.3397503f, summary.getTotal());
    }

    @Test
    public void testParseWithA10_ReturnNull() {
        File batteryFile = new File(BATTERY_A10_FILE_PATH);
        PerformanceTestResult parsedResult = new AndroidBatteryInfoResultParser()
                .parse(createPerformanceTestResultForTest(batteryFile));
        AndroidBatteryInfo summary = (AndroidBatteryInfo) parsedResult.getResultSummary();

        Assertions.assertNotNull(parsedResult);
        Assertions.assertEquals(0.0230f, summary.getAppUsage());
        Assertions.assertEquals(4.35f, summary.getTotal());
    }

    private PerformanceTestResult createPerformanceTestResultForTest(File batteryFile) {
        PerformanceTestResult performanceTestResult = new PerformanceTestResult();
        performanceTestResult.performanceInspectionResults = new CopyOnWriteArrayList<>();
        performanceTestResult.performanceInspectionResults.add(new PerformanceInspectionResult(batteryFile,
                PerformanceInspection.createAndroidBatteryInfoInspection("", "", "sample test")
        ));
        return performanceTestResult;
    }
}
