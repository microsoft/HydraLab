// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.t2c;

import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.agent.runner.appium.AppiumRunner;
import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestRunDeviceCombo;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.AppiumServerManager;
import com.microsoft.hydralab.common.management.device.DeviceType;
import com.microsoft.hydralab.common.util.ThreadUtils;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import com.microsoft.hydralab.t2c.runner.ActionInfo;
import com.microsoft.hydralab.t2c.runner.DriverInfo;
import com.microsoft.hydralab.t2c.runner.T2CAppiumUtils;
import com.microsoft.hydralab.t2c.runner.T2CJsonParser;
import com.microsoft.hydralab.t2c.runner.TestInfo;
import com.microsoft.hydralab.t2c.runner.controller.AndroidDriverController;
import com.microsoft.hydralab.t2c.runner.controller.BaseDriverController;
import com.microsoft.hydralab.t2c.runner.controller.EdgeDriverController;
import com.microsoft.hydralab.t2c.runner.controller.IOSDriverController;
import com.microsoft.hydralab.t2c.runner.controller.WindowsDriverController;
import io.appium.java_client.windows.WindowsDriver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class T2CRunner extends AppiumRunner {
    private static final int MAJOR_APPIUM_VERSION = 1;
    private static final int MINOR_APPIUM_VERSION = -1;
    private static final int MAJOR_FFMPEG_VERSION = 4;
    private static final int MINOR_FFMPEG_VERSION = -1;
    String agentName;
    private String pkgName;
    private int currentIndex = 0;

    public T2CRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                     TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                     PerformanceTestManagementService performanceTestManagementService, String agentName) {
        super(agentManagementService, testTaskRunCallback, testRunDeviceOrchestrator, performanceTestManagementService);
        this.agentName = agentName;
    }

    @Override
    protected List<EnvCapabilityRequirement> getEnvCapabilityRequirements() {
        return List.of(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.appium, MAJOR_APPIUM_VERSION, MINOR_APPIUM_VERSION),
                new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.ffmpeg, MAJOR_FFMPEG_VERSION, MINOR_FFMPEG_VERSION));
    }

    @Override
    protected File runAndGetGif(File initialJsonFile, String unusedSuiteName, TestRunDevice testRunDevice, TestTask testTask,
                                TestRun testRun, File deviceTestResultFolder, Logger logger) {
        pkgName = testTask.getPkgName();
        // Test start
        testRunDeviceOrchestrator.startScreenRecorder(testRunDevice, deviceTestResultFolder, testTask.getTimeOutSecond(), logger);
        long recordingStartTimeMillis = System.currentTimeMillis();

        testRunDeviceOrchestrator.startLogCollector(testRunDevice, pkgName, testRun, logger);
        testRunDeviceOrchestrator.startGifEncoder(testRunDevice, testRun.getResultFolder(), pkgName + ".gif");
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(testRunDevice.getLogPath())));
        testRun.setTotalCount(testTask.testJsonFileList.size() + (initialJsonFile == null ? 0 : 1));
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        testRun.addNewTimeTag("testRunStarted", System.currentTimeMillis() - recordingStartTimeMillis);

        performanceTestManagementService.testRunStarted();

        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, pkgName.substring(pkgName.lastIndexOf('.') + 1) + ".testRunStarted");
        currentIndex = 0;

        if (initialJsonFile != null) {
            runT2CJsonTestCase(initialJsonFile, testRunDevice, testRun, logger, recordingStartTimeMillis);
        }
        for (File jsonFile : testTask.testJsonFileList) {
            runT2CJsonTestCase(jsonFile, testRunDevice, testRun, logger, recordingStartTimeMillis);
        }

        // Test finish
        logger.info(pkgName + ".end");
        performanceTestManagementService.testRunFinished();
        testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.onTestEnded();
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, null);
        testRunDeviceOrchestrator.stopGitEncoder(testRunDevice, agentManagementService.getScreenshotDir(), logger);
        testRunDeviceOrchestrator.stopScreenRecorder(testRunDevice, testRun.getResultFolder(), logger);
        testRunDeviceOrchestrator.stopLogCollector(testRunDevice);
        return testRunDevice.getGifFile();
    }

    private void runT2CJsonTestCase(File jsonFile, TestRunDevice testRunDevice, TestRun testRun,
                                    Logger logger, long recordingStartTimeMillis) {
        AndroidTestUnit ongoingTest = new AndroidTestUnit();
        ongoingTest.setNumtests(testRun.getTotalCount());
        ongoingTest.setStartTimeMillis(System.currentTimeMillis());
        ongoingTest.setRelStartTimeInVideo(ongoingTest.getStartTimeMillis() - recordingStartTimeMillis);
        ongoingTest.setCurrentIndexNum(currentIndex++);
        ongoingTest.setTestName(jsonFile.getName());
        ongoingTest.setTestedClass(pkgName);
        ongoingTest.setDeviceTestResultId(testRun.getId());
        ongoingTest.setTestTaskId(testRun.getTestTaskId());

        logger.info(ongoingTest.getTitle());

        testRun.addNewTimeTag(currentIndex + ". " + ongoingTest.getTitle(),
                System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.addNewTestUnit(ongoingTest);

        performanceTestManagementService.testStarted(ongoingTest.getTitle());

        testRunDeviceOrchestrator.addGifFrameAsyncDelay(testRunDevice, agentManagementService.getScreenshotDir(), 5, logger);

        // Run Test
        try {
            runAppiumT2CTest(testRunDevice, jsonFile, logger);
            performanceTestManagementService.testSuccess(ongoingTest.getTitle());
            ongoingTest.setStatusCode(AndroidTestUnit.StatusCodes.OK);
            ongoingTest.setSuccess(true);
        } catch (Exception e) {
            // Fail
            ongoingTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
            ongoingTest.setSuccess(false);
            ongoingTest.setStack(e.toString());
            performanceTestManagementService.testFailure(ongoingTest.getTitle());
            testRun.addNewTimeTag(ongoingTest.getTitle() + ".fail",
                    System.currentTimeMillis() - recordingStartTimeMillis);
            testRun.oneMoreFailure();
        }
        ongoingTest.setEndTimeMillis(System.currentTimeMillis());
        ongoingTest.setRelEndTimeInVideo(ongoingTest.getEndTimeMillis() - recordingStartTimeMillis);
        testRun.addNewTimeTag(ongoingTest.getTitle() + ".end",
                System.currentTimeMillis() - recordingStartTimeMillis);
    }

    private void runAppiumT2CTest(TestRunDevice testRunDevice, File jsonFile, Logger reportLogger) {
        reportLogger.info("Start T2C Test");
        T2CJsonParser t2CJsonParser = new T2CJsonParser(reportLogger);
        TestInfo testInfo = t2CJsonParser.parseJsonFile(jsonFile.getAbsolutePath());
        Assert.notNull(testInfo, "Failed to parse the json file for test automation.");

        Map<String, BaseDriverController> driverControllerMap = new HashMap<>();
        Map<String, Integer> deviceCountMap = new HashMap<>();

        try {
            AppiumServerManager appiumServerManager = testRunDeviceOrchestrator.getAppiumServerManager();
            // Prepare drivers
            for (DriverInfo driverInfo : testInfo.getDrivers()) {
                DeviceInfo deviceInfo;
                if (driverInfo.getPlatform().equalsIgnoreCase(DeviceType.ANDROID.name())) {
                    deviceInfo = getDeviceByType(testRunDevice, DeviceType.ANDROID.name(), deviceCountMap);
                    AndroidDriverController androidDriverController = new AndroidDriverController(
                            appiumServerManager.getAndroidDriver(deviceInfo, reportLogger),
                            deviceInfo.getSerialNum(), reportLogger);
                    driverControllerMap.put(driverInfo.getId(), androidDriverController);
                    reportLogger.info("Successfully init an Android driver: " + deviceInfo.getSerialNum());
                }
                if (driverInfo.getPlatform().equalsIgnoreCase(DeviceType.WINDOWS.name())) {
                    WindowsDriver windowsDriver;
                    String testWindowsApp = driverInfo.getLauncherApp();
                    if (testWindowsApp.length() > 0 && !"root".equalsIgnoreCase(testWindowsApp)) {
                        windowsDriver = appiumServerManager.getWindowsAppDriver(testWindowsApp, reportLogger);
                    } else {
                        testWindowsApp = "Root";
                        windowsDriver = appiumServerManager.getWindowsRootDriver(reportLogger);
                    }
                    driverControllerMap.put(driverInfo.getId(),
                            new WindowsDriverController(windowsDriver, "Windows", reportLogger));

                    reportLogger.info("Successfully init a Windows driver: " + testWindowsApp);
                }
                if (driverInfo.getPlatform().equalsIgnoreCase("browser")) {
                    appiumServerManager.getEdgeDriver(reportLogger);
                    if (!StringUtils.isEmpty(driverInfo.getInitURL())) {
                        appiumServerManager.getEdgeDriver(reportLogger).get(driverInfo.getInitURL());
                    }
                    // Waiting for loading url
                    ThreadUtils.safeSleep(5000);
                    driverControllerMap.put(driverInfo.getId(), new EdgeDriverController(
                            appiumServerManager.getWindowsEdgeDriver(reportLogger),
                            appiumServerManager.getEdgeDriver(reportLogger),
                            "Edge", reportLogger));
                    reportLogger.info("Successfully init a Edge driver");
                }
                if (driverInfo.getPlatform().equalsIgnoreCase(DeviceType.IOS.name())) {
                    deviceInfo = getDeviceByType(testRunDevice, DeviceType.IOS.name(), deviceCountMap);
                    IOSDriverController iosDriverController = new IOSDriverController(
                            appiumServerManager.getIOSDriver(deviceInfo, reportLogger),
                            deviceInfo.getSerialNum(), reportLogger);
                    driverControllerMap.put(driverInfo.getId(), iosDriverController);
                }
            }

            ArrayList<ActionInfo> caseList = testInfo.getActions();

            for (ActionInfo actionInfo : caseList) {
                BaseDriverController driverController = driverControllerMap.get(actionInfo.getDriverId());
                reportLogger.info("Start step: " + actionInfo.getId() + ", description: " + actionInfo.getDescription() + "action: " + actionInfo.getActionType() + " on element: "
                        + (actionInfo.getTestElement() != null ? actionInfo.getTestElement().getElementInfo() : "No Element"));
                T2CAppiumUtils.doAction(driverController, actionInfo, reportLogger);
            }
        } catch (Exception e) {
            reportLogger.error("T2C Test Error: ", e);
            throw e;
        } finally {
            reportLogger.info("Finish T2C Test");
        }
    }

    private DeviceInfo getDeviceByType(TestRunDevice testRunDevice, String type, Map<String, Integer> deviceCountMap) {
        if (!(testRunDevice instanceof TestRunDeviceCombo)) {
            return testRunDevice.getDeviceInfo();
        }
        String tag = type + "_" + deviceCountMap.getOrDefault(type, 0);
        DeviceInfo deviceInfo = ((TestRunDeviceCombo) testRunDevice).getDeviceByTag(tag);
        if (deviceInfo != null && !DeviceType.WINDOWS.name().equals(type)) {
            deviceCountMap.put(type, deviceCountMap.getOrDefault(type, 0) + 1);
        }
        return deviceInfo;
    }
}