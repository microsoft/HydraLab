package com.microsoft.hydralab.appium;

import com.microsoft.hydralab.TestRunThreadContext;
import com.microsoft.hydralab.performance.PerformanceInspectionService;
import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import org.junit.jupiter.api.Test;

import java.util.List;


/**
 * Sample of performance test. Will not check in
 */
public class PerformanceTestSample {
    @Test
    public void performanceTestCase() {
        String appIdAndroid = "com.mocrosoft.appmanager";
        String androidDeviceId = "Android";
        String appIdWindows = "Microsoft.YourPhone_8wekyb3d8bbwe";
        String windowsDeviceId = "Windows";

        PerformanceInspectionService performanceInspectionService = PerformanceInspectionService.getInstance();

        PerformanceInspection androidBatteryInfoSpec = PerformanceInspection.createAndroidBatteryInfoSpec(appIdAndroid, androidDeviceId);
        PerformanceInspection androidMemoryDumpSpec = PerformanceInspection.createAndroidMemoryDumpSpec(appIdAndroid, androidDeviceId);
        PerformanceInspection androidMemoryInfoSpec = PerformanceInspection.createAndroidMemoryInfoSpec(appIdAndroid, androidDeviceId);
        PerformanceInspection windowsBatteryInfoSpec = PerformanceInspection.createWindowsBatteryInfoSpec(appIdWindows, windowsDeviceId);
        PerformanceInspection windowsMemoryInfoSpec = PerformanceInspection.createWindowsMemoryInfoSpec(appIdWindows, windowsDeviceId);

        performanceInspectionService.reset(androidBatteryInfoSpec);
        performanceInspectionService.reset(androidMemoryDumpSpec);
        performanceInspectionService.reset(androidMemoryInfoSpec);
        performanceInspectionService.reset(windowsBatteryInfoSpec);
        performanceInspectionService.reset(windowsMemoryInfoSpec);

        //testing...
        System.out.println("Start LTW...");
        performanceInspectionService.inspect(androidBatteryInfoSpec.rename("Start LTW"));
        performanceInspectionService.inspect(androidMemoryDumpSpec);

        System.out.println("Start PL...");
        performanceInspectionService.inspect(windowsMemoryInfoSpec.rename("Start PL"));
        performanceInspectionService.inspect(windowsBatteryInfoSpec);

        List<PerformanceTestResult> performanceTestResults = performanceInspectionService.parse();

    }
}
