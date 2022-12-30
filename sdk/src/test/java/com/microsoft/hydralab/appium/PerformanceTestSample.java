package com.microsoft.hydralab.appium;

import com.microsoft.hydralab.performance.PerformanceInspectionService;
import com.microsoft.hydralab.performance.PerformanceTestSpec;
import org.junit.jupiter.api.Test;


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

        PerformanceTestSpec androidBatteryInfoSpec = PerformanceTestSpec.createAndroidBatteryInfoSpec(appIdAndroid, androidDeviceId);
        PerformanceTestSpec androidMemoryDumpSpec = PerformanceTestSpec.createAndroidMemoryDumpSpec(appIdAndroid, androidDeviceId);
        PerformanceTestSpec androidMemoryInfoSpec = PerformanceTestSpec.createAndroidMemoryInfoSpec(appIdAndroid, androidDeviceId);
        PerformanceTestSpec windowsBatteryInfoSpec = PerformanceTestSpec.createWindowsBatteryInfoSpec(appIdWindows, windowsDeviceId);
        PerformanceTestSpec windowsMemoryInfoSpec = PerformanceTestSpec.createWindowsMemoryInfoSpec(appIdWindows, windowsDeviceId);

        performanceInspectionService.initialize(androidBatteryInfoSpec);
        performanceInspectionService.initialize(androidMemoryDumpSpec);
        performanceInspectionService.initialize(androidMemoryInfoSpec);
        performanceInspectionService.initialize(windowsBatteryInfoSpec);
        performanceInspectionService.initialize(windowsMemoryInfoSpec);

        //testing...
        System.out.println("Start LTW...");
        performanceInspectionService.inspect(androidBatteryInfoSpec.rename("Start LTW"));
        performanceInspectionService.inspect(androidMemoryDumpSpec);

        System.out.println("Start PL...");
        performanceInspectionService.inspect(windowsMemoryInfoSpec.rename("Start PL"));
        performanceInspectionService.inspect(windowsBatteryInfoSpec);

    }
}
