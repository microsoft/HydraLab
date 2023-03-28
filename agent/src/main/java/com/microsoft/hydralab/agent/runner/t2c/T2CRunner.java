// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.t2c;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.img.gif.AnimatedGifEncoder;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.agent.runner.appium.AppiumRunner;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.AppiumServerManager;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.ThreadUtils;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import com.microsoft.hydralab.t2c.runner.*;
import com.microsoft.hydralab.t2c.runner.controller.*;
import io.appium.java_client.windows.WindowsDriver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class T2CRunner extends AppiumRunner {

    private final AnimatedGifEncoder e = new AnimatedGifEncoder();
    private LogCollector logCollector;
    private ScreenRecorder deviceScreenRecorder;
    private String pkgName;
    String agentName;
    private int currentIndex = 0;

    public T2CRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                     PerformanceTestManagementService performanceTestManagementService, String agentName) {
        super(agentManagementService, testTaskRunCallback, performanceTestManagementService);
        this.agentName = agentName;
    }

    @Override
    protected File runAndGetGif(File initialJsonFile, String unusedSuiteName, DeviceInfo deviceInfo,
                                TestTask testTask,
                                TestRun testRun, File deviceTestResultFolder, Logger reportLogger) {
        pkgName = testTask.getPkgName();

        // Test start
        deviceScreenRecorder = testDeviceManager.getScreenRecorder(deviceInfo, deviceTestResultFolder, reportLogger);
        deviceScreenRecorder.setupDevice();
        deviceScreenRecorder.startRecord(testTask.getTimeOutSecond());
        long recordingStartTimeMillis = System.currentTimeMillis();

        logCollector = testDeviceManager.getLogCollector(deviceInfo, pkgName, testRun, reportLogger);
        logCollector.start();

        testRun.setTotalCount(testTask.testJsonFileList.size() + (initialJsonFile == null ? 0 : 1));
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        testRun.addNewTimeTag("testRunStarted", System.currentTimeMillis() - recordingStartTimeMillis);

        performanceTestManagementService.testRunStarted();

        deviceInfo.setRunningTestName(pkgName.substring(pkgName.lastIndexOf('.') + 1) + ".testRunStarted");
        currentIndex = 0;

        File gifFile = new File(testRun.getResultFolder(), pkgName + ".gif");
        e.start(gifFile.getAbsolutePath());
        e.setDelay(1000);
        e.setRepeat(0);

        if (initialJsonFile != null) {
            runT2CJsonTestCase(initialJsonFile, deviceInfo, testRun, reportLogger, recordingStartTimeMillis);
        }
        for (File jsonFile : testTask.testJsonFileList) {
            runT2CJsonTestCase(jsonFile, deviceInfo, testRun, reportLogger, recordingStartTimeMillis);
        }

        // Test finish
        reportLogger.info(pkgName + ".end");
        performanceTestManagementService.testRunFinished();
        testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.onTestEnded();
        deviceInfo.setRunningTestName(null);
        releaseResource();
        return gifFile;
    }

    @Override
    public TestRun createTestRun(DeviceInfo deviceInfo, TestTask testTask, Logger parentLogger) {
        TestRun testRun = super.createTestRun(deviceInfo, testTask, parentLogger);
        String deviceName =
                System.getProperties().getProperty("os.name") + "-" + agentName + "-" + deviceInfo.getName();
        testRun.setDeviceName(deviceName);
        return testRun;
    }

    private void runT2CJsonTestCase(File jsonFile, DeviceInfo deviceInfo, TestRun testRun,
                                    Logger reportLogger, long recordingStartTimeMillis) {
        AndroidTestUnit ongoingTest = new AndroidTestUnit();
        ongoingTest.setNumtests(testRun.getTotalCount());
        ongoingTest.setStartTimeMillis(System.currentTimeMillis());
        ongoingTest.setRelStartTimeInVideo(ongoingTest.getStartTimeMillis() - recordingStartTimeMillis);
        ongoingTest.setCurrentIndexNum(currentIndex++);
        ongoingTest.setTestName(jsonFile.getName());
        ongoingTest.setTestedClass(pkgName);
        ongoingTest.setDeviceTestResultId(testRun.getId());
        ongoingTest.setTestTaskId(testRun.getTestTaskId());

        reportLogger.info(ongoingTest.getTitle());

        testRun.addNewTimeTag(currentIndex + ". " + ongoingTest.getTitle(),
                System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.addNewTestUnit(ongoingTest);

        performanceTestManagementService.testStarted(ongoingTest.getTitle());

        testDeviceManager.updateScreenshotImageAsyncDelay(deviceInfo, TimeUnit.SECONDS.toMillis(5),
                (imagePNGFile -> {
                    if (imagePNGFile == null || !e.isStarted()) {
                        return;
                    }
                    try {
                        e.addFrame(ImgUtil.toBufferedImage(ImgUtil.scale(ImageIO.read(imagePNGFile), 0.3f)));
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }), reportLogger);

        // Run Test
        try {
            runAppiumT2CTest(deviceInfo, jsonFile, reportLogger);
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

    private void releaseResource() {
        e.finish();
        deviceScreenRecorder.finishRecording();
        logCollector.stopAndAnalyse();
    }

    private void runAppiumT2CTest(DeviceInfo deviceInfo, File jsonFile, Logger reportLogger) {
        reportLogger.info("Start T2C Test");
        T2CJsonParser t2CJsonParser = new T2CJsonParser(reportLogger);
        TestInfo testInfo = t2CJsonParser.parseJsonFile(jsonFile.getAbsolutePath());
        Assert.notNull(testInfo, "Failed to parse the json file for test automation.");

        Map<String, BaseDriverController> driverControllerMap = new HashMap<>();

        try {
            // TODO: Need to get appium service manager from the device info
            AppiumServerManager appiumServerManager = testDeviceManager.getAppiumServerManager();
            // Prepare drivers
            for (DriverInfo driverInfo : testInfo.getDrivers()) {
                if (driverInfo.getPlatform().equalsIgnoreCase("android")) {
                    AndroidDriverController androidDriverController = new AndroidDriverController(
                            appiumServerManager.getAndroidDriver(deviceInfo, reportLogger),
                            deviceInfo.getSerialNum(), reportLogger);
                    driverControllerMap.put(driverInfo.getId(), androidDriverController);
                    reportLogger.info("Successfully init an Android driver: " + deviceInfo.getSerialNum());
                }
                if (driverInfo.getPlatform().equalsIgnoreCase("windows")) {
                    WindowsDriver windowsDriver;
                    String testWindowsApp = driverInfo.getLauncherApp();
                    if (testWindowsApp.length() > 0 && !testWindowsApp.equalsIgnoreCase("root")) {
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
                if (driverInfo.getPlatform().equalsIgnoreCase("iOS")) {
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
}
