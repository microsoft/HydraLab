package com.microsoft.hydralab.common.service;

import com.microsoft.hydralab.agent.runner.TestRunThreadContext;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionService;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * Just for self-testing during development for now.
 * Maybe we can consider rewriting this class to UT in the near future.
 */

public class PerformanceTestManagementServiceTest {
    @Test
    public void performanceTestCase() {
        String appIdWindows = "Microsoft.YourPhone_8wekyb3d8bbwe";

        TestRun testRun = new TestRun("deviceSerialNumber-1", "deviceName-1", "testTaskId-1");
        File deviceTestResultFolder = new File("E:\\code\\HydraLab\\build", "SerialNum-1");
        File performanceFolder = new File(deviceTestResultFolder, "performance");
        performanceFolder.delete();
        testRun.setResultFolder(deviceTestResultFolder);
        TestRunThreadContext.init(testRun);

        PerformanceTestManagementService performanceTestManagementService = new PerformanceTestManagementService();
        performanceTestManagementService.initialize();
        PerformanceInspectionService performanceInspectionService = PerformanceInspectionService.getInstance();

        PerformanceInspection windowsBatteryInfoSpec = PerformanceInspection.createWindowsBatteryInspection(appIdWindows, "deviceIdentifier-battery", "description-battery");
        PerformanceInspection windowsMemoryInfoSpec = PerformanceInspection.createWindowsMemoryInspection(appIdWindows, "deviceIdentifier-memory", "description-memory");

        System.out.println("Start PL...");
        //performanceInspectionService.inspect(windowsBatteryInfoSpec);
        performanceInspectionService.inspect(windowsMemoryInfoSpec);

        // List<PerformanceTestResult> performanceTestResults = performanceInspectionService.parse();
    }
}