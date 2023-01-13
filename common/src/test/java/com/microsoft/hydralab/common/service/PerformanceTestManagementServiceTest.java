package com.microsoft.hydralab.common.service;

import com.microsoft.hydralab.TestRunThreadContext;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionService;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;

/**
 * Just for self-testing during development for now.
 * Maybe we can consider rewriting this class to UT in the near future.
 */

public class PerformanceTestManagementServiceTest {
    @Test
    public void performanceTestCase() {
        String appIdAndroid = "com.mocrosoft.appmanager";
        String androidDeviceId = "Android";
        String appIdWindows = "Microsoft.YourPhone_8wekyb3d8bbwe";
        String windowsDeviceId = "Windows";

        TestRun testRun = new TestRun("deviceSerialNumber-1", "deviceName-1", "testTaskId-1");
        File deviceTestResultFolder = new File("C:\\code\\HydraLab\\build", "SerialNum-1");
        File performanceFolder = new File(deviceTestResultFolder, "performance");
        performanceFolder.delete();
        testRun.setTestRunResultFolder(deviceTestResultFolder);
        TestRunThreadContext.init(testRun, null, null);

        PerformanceTestManagementService performanceTestManagementService = new PerformanceTestManagementService();
        performanceTestManagementService.initialize();
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

        System.out.println("Start PL...");
        performanceInspectionService.inspect(windowsMemoryInfoSpec.rename("Start PL"));
        performanceInspectionService.inspect(windowsBatteryInfoSpec);

        //testing...
        System.out.println("Start LTW...");
        performanceInspectionService.inspect(androidBatteryInfoSpec.rename("Start LTW"));
        performanceInspectionService.inspect(androidMemoryDumpSpec);

        List<PerformanceTestResult> performanceTestResults = performanceInspectionService.parse();
    }

    protected TestRun createTestRun(DeviceInfo deviceInfo, TestTask testTask, Logger parentLogger) {
        TestRun testRun = new TestRun(deviceInfo.getSerialNum(), deviceInfo.getName(), testTask.getId());
        File deviceTestResultFolder = new File(testTask.getResourceDir(), deviceInfo.getSerialNum());
        parentLogger.info("DeviceTestResultFolder {}", deviceTestResultFolder);
        if (!deviceTestResultFolder.exists()) {
            if (!deviceTestResultFolder.mkdirs()) {
                throw new RuntimeException("deviceTestResultFolder.mkdirs() failed: " + deviceTestResultFolder);
            }
        }

        testRun.setTestRunResultFolder(deviceTestResultFolder);
        testTask.addTestedDeviceResult(testRun);
        return testRun;
    }
}