// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.t2c;

import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.agent.runner.appium.AppiumRunner;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.slf4j.Logger;

import java.io.File;

public class T2CRunner extends AppiumRunner {

    private TestRunDevice testRunDevice;
    private String pkgName;
    String agentName;
    private int currentIndex = 0;

    public T2CRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                     TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                     PerformanceTestManagementService performanceTestManagementService, String agentName) {
        super(agentManagementService, testTaskRunCallback, testRunDeviceOrchestrator, performanceTestManagementService);
        this.agentName = agentName;
    }

    @Override
    protected File runAndGetGif(File initialJsonFile, String unusedSuiteName, TestRunDevice testRunDevice, TestTask testTask,
                                TestRun testRun, File deviceTestResultFolder, Logger logger) {
        pkgName = testTask.getPkgName();
        this.testRunDevice = testRunDevice;
        // Test start
        testRunDeviceOrchestrator.startScreenRecorder(testRunDevice, deviceTestResultFolder, testTask.getTimeOutSecond(), logger);
        long recordingStartTimeMillis = System.currentTimeMillis();

        testRunDeviceOrchestrator.startLogCollector(testRunDevice, pkgName, testRun, logger);
        testRunDeviceOrchestrator.startGifEncoder(testRunDevice, testRun.getResultFolder(), pkgName + ".gif");
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
            //testRunDeviceOrchestrator.runAppiumT2CTest(testRunDevice, jsonFile, logger);
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
}
