// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.maestro;

import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.performance.PerformanceTestListener;
import org.slf4j.Logger;

import java.io.File;

/**
 * @author zhoule
 * @date 07/19/2023
 */

public class MaestroListener {
    private final TestRunDevice testRunDevice;
    private final TestRun testRun;
    private final TestTask testTask;
    private final Logger logger;
    private final String pkgName;
    private final AgentManagementService agentManagementService;
    private final PerformanceTestListener performanceTestListener;
    private TestRunDeviceOrchestrator testRunDeviceOrchestrator;
    private long recordingStartTimeMillis;
    private int index;
    private boolean alreadyEnd = false;
    private AndroidTestUnit ongoingTestUnit;

    public MaestroListener(AgentManagementService agentManagementService,
                           TestRunDevice testRunDevice, TestRun testRun, TestTask testTask,
                           TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                           PerformanceTestListener performanceTestListener) {
        this.testRunDevice = testRunDevice;
        this.testRunDeviceOrchestrator = testRunDeviceOrchestrator;
        this.agentManagementService = agentManagementService;
        this.testRun = testRun;
        this.testTask = testTask;
        this.logger = testRun.getLogger();
        this.pkgName = testTask.getPkgName();
        this.performanceTestListener = performanceTestListener;
    }

    public void testRunStarted() {
        infoLogEnter("testRunStarted", "maestro test");

        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        testRun.addNewTimeTag("testRunStarted", System.currentTimeMillis() - recordingStartTimeMillis);
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, "MaestroTest.testRunStarted");
        performanceTestListener.testRunStarted();
        performanceTestListener.testStarted("MaestroTestCase" + index);
        testRunDeviceOrchestrator.addGifFrameAsyncDelay(testRunDevice, agentManagementService.getScreenshotDir(), 5, logger);
    }

    private void initUnitCase(String caseName, int testSeconds) {
        final int unitIndex = index;
        ongoingTestUnit = new AndroidTestUnit();
        ongoingTestUnit.setNumtests(index);
        ongoingTestUnit.setStartTimeMillis(System.currentTimeMillis() - testSeconds * 1000);
        ongoingTestUnit.setRelStartTimeInVideo(ongoingTestUnit.getStartTimeMillis() - recordingStartTimeMillis);
        ongoingTestUnit.setCurrentIndexNum(unitIndex);
        ongoingTestUnit.setTestName(caseName);
        ongoingTestUnit.setTestedClass("MaestroTest");
        ongoingTestUnit.setDeviceTestResultId(testRun.getId());
        ongoingTestUnit.setTestTaskId(testRun.getTestTaskId());

        testRun.addNewTestUnit(ongoingTestUnit);
        testRun.addNewTimeTag(unitIndex + ". " + ongoingTestUnit.getTitle(),
                System.currentTimeMillis() - testSeconds * 1000 - recordingStartTimeMillis);
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, ongoingTestUnit.getTitle());
        testRunDeviceOrchestrator.addGifFrameAsyncDelay(testRunDevice, agentManagementService.getScreenshotDir(), 5, logger);
        performanceTestListener.testStarted("MaestroTestCase" + index);
    }

    public void startRecording(int maxTime) {
        logger.info("Start record screen");
        if (!testTask.isDisableRecording()) {
            testRunDeviceOrchestrator.startScreenRecorder(testRunDevice, testRun.getResultFolder(), maxTime <= 0 ? 30 * 60 : maxTime, logger);
        }
        logger.info("Start gif frames collection");
        testRunDeviceOrchestrator.startGifEncoder(testRunDevice, testRun.getResultFolder(), pkgName + ".gif");
        logger.info("Start logcat collection");
        testRunDeviceOrchestrator.startLogCollector(testRunDevice, pkgName, testRun, logger);
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(testRunDevice.getLogPath())));
        recordingStartTimeMillis = System.currentTimeMillis();
        final String initializing = "Initializing";
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, initializing);
        testRun.addNewTimeTag(initializing, 0);
    }

    public void testFailed(String caseName, int testSeconds, String error) {
        errorLogEnter("testFailed", caseName);
        performanceTestListener.testFailure("MaestroTestCase" + index);
        index++;
        initUnitCase(caseName, testSeconds);
        ongoingTestUnit.setStack(error);
        ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
        testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".fail", System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".end", System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.oneMoreFailure();
        ongoingTestUnit.setSuccess(false);
        ongoingTestUnit.setEndTimeMillis(System.currentTimeMillis());
        ongoingTestUnit.setRelEndTimeInVideo(ongoingTestUnit.getEndTimeMillis() - recordingStartTimeMillis);
    }

    public void testEnded(String caseName, int testSeconds) {
        infoLogEnter("testEnded", caseName);
        performanceTestListener.testSuccess("MaestroTestCase" + index);
        index++;
        initUnitCase(caseName, testSeconds);
        testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".end",
                System.currentTimeMillis() - recordingStartTimeMillis);
        ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.OK);
        ongoingTestUnit.setSuccess(true);
        ongoingTestUnit.setEndTimeMillis(System.currentTimeMillis());
        ongoingTestUnit.setRelEndTimeInVideo(ongoingTestUnit.getEndTimeMillis() - recordingStartTimeMillis);
    }

    public void testRunFailed(String outputPath) {
        errorLogEnter("testRunFailed", "Start to copy output files", outputPath);
        File file = new File(outputPath);
        if (!file.exists()) {
            logger.info("testRunFailed: " + outputPath + " not exist");
            return;
        }
        File copiedFile = new File(testRun.getResultFolder(), file.getName());
        copiedFile.mkdir();
        FileUtil.copyFile(outputPath, copiedFile.getAbsolutePath());
    }

    public void testRunEnded() {
        testRun.setTotalCount(index);
        infoLogEnter("testRunEnded", Thread.currentThread().getName());
        synchronized (this) {
            if (alreadyEnd) {
                return;
            }
            performanceTestListener.testRunFinished();
            testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
            testRun.onTestEnded();
            testRunDeviceOrchestrator.setRunningTestName(testRunDevice, null);
            testRunDeviceOrchestrator.stopGitEncoder(testRunDevice, agentManagementService.getScreenshotDir(), logger);
            if (!testTask.isDisableRecording()) {
                testRunDeviceOrchestrator.stopScreenRecorder(testRunDevice, testRun.getResultFolder(), logger);
            }
            testRunDeviceOrchestrator.stopLogCollector(testRunDevice);
            alreadyEnd = true;
        }
    }

    private void infoLogEnter(Object... args) {
        StringBuilder builder = new StringBuilder();
        for (Object arg : args) {
            builder.append(" >").append(arg);
        }
        logger.info("TestRunListener: {}", builder);
    }

    private void errorLogEnter(Object... args) {
        StringBuilder builder = new StringBuilder();
        for (Object arg : args) {
            builder.append(" >").append(arg);
        }
        logger.error("TestRunListener: {}", builder);
    }

    public File getGifFile() {
        return testRunDevice.getGifFile();
    }
}
