// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.monkey;

import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.agent.runner.appium.AppiumRunner;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.util.IOSUtils;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import io.appium.java_client.appmanagement.ApplicationState;
import io.appium.java_client.ios.IOSDriver;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Random;

public class AppiumMonkeyRunner extends AppiumRunner {

    public AppiumMonkeyRunner(AgentManagementService agentManagementService,
                              TestTaskRunCallback testTaskRunCallback,
                              TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                              PerformanceTestManagementService performanceTestManagementService) {
        super(agentManagementService, testTaskRunCallback, testRunDeviceOrchestrator, performanceTestManagementService);
    }

    @Override
    protected File runAndGetGif(File appiumJarFile, String appiumCommand, TestRunDevice testRunDevice, TestTask testTask,
                                TestRun testRun, File deviceTestResultFolder, Logger logger) {
        String pkgName = testTask.getPkgName();

        long recordingStartTimeMillis = System.currentTimeMillis();

        if (!testTask.isDisableRecording()) {
            testRunDeviceOrchestrator.startScreenRecorder(testRunDevice, deviceTestResultFolder, testTask.getTimeOutSecond(), logger);
        }
        testRunDeviceOrchestrator.startLogCollector(testRunDevice, pkgName, testRun, logger);
        testRunDeviceOrchestrator.startGifEncoder(testRunDevice, testRun.getResultFolder(), pkgName + ".gif");
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(testRunDevice.getLogPath())));
        testRun.setTotalCount(1);

        AndroidTestUnit ongoingMonkeyTest = new AndroidTestUnit();
        ongoingMonkeyTest.setNumtests(testRun.getTotalCount());
        ongoingMonkeyTest.setStartTimeMillis(System.currentTimeMillis());
        ongoingMonkeyTest.setRelStartTimeInVideo(ongoingMonkeyTest.getStartTimeMillis() - recordingStartTimeMillis);
        ongoingMonkeyTest.setCurrentIndexNum(1);
        ongoingMonkeyTest.setTestName("Appium_Monkey_Test");
        ongoingMonkeyTest.setTestedClass(pkgName);
        ongoingMonkeyTest.setDeviceTestResultId(testRun.getId());
        ongoingMonkeyTest.setTestTaskId(testRun.getTestTaskId());
        testRun.addNewTestUnit(ongoingMonkeyTest);

        logger.info(ongoingMonkeyTest.getTitle());

        performanceTestManagementService.testRunStarted();

        testRun.addNewTimeTag(1 + ". " + ongoingMonkeyTest.getTitle(),
                System.currentTimeMillis() - recordingStartTimeMillis);
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, ongoingMonkeyTest.getTitle());
        testRunDeviceOrchestrator.addGifFrameAsyncDelay(testRunDevice, agentManagementService.getScreenshotDir(), 5, logger);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());

        performanceTestManagementService.testStarted(ongoingMonkeyTest.getTitle());

        runAppiumMonkey(testRunDevice, pkgName, testTask.getMaxStepCount(), logger);

        testRunDeviceOrchestrator.stopGitEncoder(testRunDevice, agentManagementService.getScreenshotDir(), logger);
        if (!testTask.isDisableRecording()) {
            testRunDeviceOrchestrator.stopScreenRecorder(testRunDevice, testRun.getResultFolder(), logger);
        }
        testRunDeviceOrchestrator.stopLogCollector(testRunDevice);

        // Success status
        if (StringUtils.isNotEmpty(testRun.getCrashStack())) {
            // Fail
            ongoingMonkeyTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
            ongoingMonkeyTest.setSuccess(false);
            ongoingMonkeyTest.setStack(testRun.getCrashStack());
            testRun.setSuccess(false);
            performanceTestManagementService.testFailure(ongoingMonkeyTest.getTitle());
            testRun.addNewTimeTagBeforeLast(ongoingMonkeyTest.getTitle() + ".fail",
                    System.currentTimeMillis() - recordingStartTimeMillis);
            testRun.oneMoreFailure();
        } else {
            // Pass
            ongoingMonkeyTest.setStatusCode(AndroidTestUnit.StatusCodes.OK);
            ongoingMonkeyTest.setSuccess(true);
            testRun.setSuccess(true);
            performanceTestManagementService.testSuccess(ongoingMonkeyTest.getTitle());
        }

        // Test finish
        logger.info(ongoingMonkeyTest.getTitle() + ".end");
        performanceTestManagementService.testRunFinished();
        ongoingMonkeyTest.setEndTimeMillis(System.currentTimeMillis());
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, null);
        testRun.addNewTimeTag(ongoingMonkeyTest.getTitle() + ".end",
                System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.onTestEnded();
        return testRunDevice.getGifFile();
    }

    public void runAppiumMonkey(TestRunDevice testRunDevice, String packageName, int round, Logger logger) {
        try {
            for (int i = 0; i < round; i++) {
                logger.info("Monkey Test Round " + i + "[Started]");
                testRunDeviceOrchestrator.getAppiumDriver(testRunDevice, logger);
                WebDriver driver = testRunDevice.getWebDriver();
                if (driver instanceof IOSDriver) {
                    IOSUtils.stopApp(testRunDevice.getDeviceInfo().getSerialNum(), packageName, logger);
                    IOSUtils.launchApp(testRunDevice.getDeviceInfo().getSerialNum(), packageName, logger);
                }
                // Select all the leaf nodes
                List<WebElement> eleList = driver.findElements(By.xpath("//*[not(*)]"));
                int count = eleList.size();
                logger.info("Found " + count + " element(s)");
                if (org.springframework.util.StringUtils.isEmpty(packageName)) {
                    if (count == 0) {
                        logger.info("No element Found, Back to Home Screen.");
                        testRunDeviceOrchestrator.backToHome(testRunDevice, logger);
                        eleList = driver.findElements(By.xpath("//*"));
                        count = eleList.size();
                    }
                } else {
                    if (count == 0 || !isAppRunningForeground(driver, packageName, logger)) {
                        logger.info(
                                "No element Found or App is Running in Background, Back to Home Screen and Restart App.");
                        testRunDeviceOrchestrator.backToHome(testRunDevice, logger);
                        IOSUtils.launchApp(testRunDevice.getDeviceInfo().getSerialNum(), packageName, logger);
                        eleList = driver.findElements(By.xpath("//*"));
                        count = eleList.size();
                    }
                }
                if (count <= 0) {
                    continue;
                }
                int r = new Random().nextInt(count);
                WebElement e = eleList.get(r);
                try {
                    String name = e.getText();
                    logger.info("Select NO. " + r + " element: " + name);
                    e.click();
                } catch (StaleElementReferenceException | ElementNotInteractableException ignore) {
                    // Cached element is not exist any more skip this try.
                }
                logger.info("Monkey Test Round " + i + "[Done]");
            }
        } catch (WebDriverException e) {
            e.printStackTrace();
            logger.info("Monkey Test Exit with Error, Quit the Driver. ");
            testRunDeviceOrchestrator.quitAppiumDriver(testRunDevice, logger);
        }
    }

    protected boolean isAppRunningForeground(WebDriver webDriver, String packageName, Logger logger) {
        if (webDriver instanceof IOSDriver) {
            ApplicationState state = ((IOSDriver) webDriver).queryAppState(packageName);
            boolean result = (state == ApplicationState.RUNNING_IN_FOREGROUND);
            if (!result) {
                logger.info("State of App " + packageName + " is: " + state.toString());
            }
            return result;
        }
        return false;
    }
}
