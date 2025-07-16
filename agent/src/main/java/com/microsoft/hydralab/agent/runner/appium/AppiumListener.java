// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.appium;

import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.performance.PerformanceTestListener;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;

import java.io.File;
import java.util.Locale;

public class AppiumListener extends RunListener {
    private final TestRunDevice testRunDevice;
    private final TestRun testRun;
    private final TestTask testTask;
    private final Logger logger;
    private final String pkgName;
    AgentManagementService agentManagementService;
    private final PerformanceTestListener performanceTestListener;
    private long recordingStartTimeMillis;
    private boolean alreadyEnd = false;
    private AndroidTestUnit ongoingTestUnit;
    private String currentTestName = "";
    private int currentTestIndex = 0;
    private TestRunDeviceOrchestrator testRunDeviceOrchestrator;

    public AppiumListener(AgentManagementService agentManagementService, TestRunDevice testRunDevice, TestRun testRun,
                          TestTask testTask, TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                          PerformanceTestListener performanceTestListener, Logger logger) {
        this.agentManagementService = agentManagementService;
        this.testRunDevice = testRunDevice;
        this.testRun = testRun;
        this.logger = logger;
        this.testTask = testTask;
        this.pkgName = testTask.getPkgName();
        this.performanceTestListener = performanceTestListener;
        this.testRunDeviceOrchestrator = testRunDeviceOrchestrator;
    }

    public File getGifFile() {
        return testRunDevice.getGifFile();
    }

    private void logEnter(Object... args) {
        StringBuilder builder = new StringBuilder();
        for (Object arg : args) {
            builder.append(" >").append(arg);
        }
        logger.info("TestRunListener: {}", builder);
        System.out.println("TestRunListener: " + builder);

    }

    public void startRecording(int maxTime) {
        logger.info("Start record screen");
        if (!testTask.isDisableRecording()) {
            testRunDeviceOrchestrator.startScreenRecorder(testRunDevice, testRun.getResultFolder(), maxTime <= 0 ? 30 * 60 : maxTime, logger);
        }
        if (!testTask.isDisableGifEncoder()) {
            logger.info("Start gif frames collection");
            testRunDeviceOrchestrator.startGifEncoder(testRunDevice, testRun.getResultFolder(), pkgName + ".gif");
        }
        logger.info("Start logcat collection");
        testRunDeviceOrchestrator.startLogCollector(testRunDevice, pkgName, testRun, logger);
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(testRunDevice.getLogPath())));
        recordingStartTimeMillis = System.currentTimeMillis();
        final String initializing = "Initializing";
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, initializing);
        testRun.addNewTimeTag(initializing, 0);
    }

    @Override
    public void testRunStarted(Description description) {
        String runName = description.getChildren().get(0).getDisplayName();
        int testCount = description.getChildren().get(0).testCount();
        logEnter("testRunStarted", runName, testCount);
        testRun.setTotalCount(testCount);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        testRun.addNewTimeTag("testRunStarted", System.currentTimeMillis() - recordingStartTimeMillis);
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, runName.substring(runName.lastIndexOf('.') + 1) + ".testRunStarted");
        logEnter(runName, description.testCount());
        performanceTestListener.testRunStarted();
    }

    @Override
    public void testStarted(Description description) {
        String testName = description.getMethodName() == null ? "testInitialization" : description.getMethodName();
        String testClassName = description.getClassName();
        String testDisplayName = description.getDisplayName();
        int testIndex;

        if (!testClassName.equals(currentTestName)) {
            currentTestName = testClassName;
        }

        testIndex = ++currentTestIndex;

        logEnter("testStarted", testDisplayName);
        ongoingTestUnit = new AndroidTestUnit();
        ongoingTestUnit.setNumtests(testIndex);

        ongoingTestUnit.setStartTimeMillis(System.currentTimeMillis());
        ongoingTestUnit.setRelStartTimeInVideo(ongoingTestUnit.getStartTimeMillis() - recordingStartTimeMillis);

        final int unitIndex = testIndex;
        ongoingTestUnit.setCurrentIndexNum(unitIndex);

//        String method = test.getTestName();
        ongoingTestUnit.setTestName(testName);

        ongoingTestUnit.setTestedClass(testClassName);

        testRun.addNewTimeTag(unitIndex + ". " + ongoingTestUnit.getTitle(),
                System.currentTimeMillis() - recordingStartTimeMillis);
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, ongoingTestUnit.getTitle());

        ongoingTestUnit.setDeviceTestResultId(testRun.getId());
        ongoingTestUnit.setTestTaskId(testRun.getTestTaskId());

        testRun.addNewTestUnit(ongoingTestUnit);

        if (!testTask.isDisableGifEncoder()) {
            testRunDeviceOrchestrator.addGifFrameAsyncDelay(testRunDevice, agentManagementService.getScreenshotDir(), 5, logger);
        }
        performanceTestListener.testStarted(ongoingTestUnit.getTitle());
    }

    @Override
    public void testFailure(Failure failure) {
        String testDisplayName = failure.getDescription().getDisplayName();
        logEnter("testFailed", testDisplayName, failure.getTrace());
        ongoingTestUnit.setStack(failure.getTrace());
        ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
        performanceTestListener.testFailure(ongoingTestUnit.getTitle());
        testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".fail",
                System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.oneMoreFailure();
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        String testDisplayName = failure.getDescription().getDisplayName();
        logEnter("testAssumptionFailure", testDisplayName, failure.getTrace());
        ongoingTestUnit.setStack(failure.getTrace());
        ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
        testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".assumptionFail",
                System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.oneMoreFailure();
    }

    @Override
    public void testIgnored(Description description) {
        logEnter("testIgnored", description.getDisplayName());
        ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.IGNORED);
    }

    @Override
    public void testFinished(Description description) {
        logEnter("testEnded", description.getDisplayName());
        testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".end",
                System.currentTimeMillis() - recordingStartTimeMillis);
        if (ongoingTestUnit.getStatusCode() == 0
                || ongoingTestUnit.getStatusCode() == AndroidTestUnit.StatusCodes.ASSUMPTION_FAILURE
                || ongoingTestUnit.getStatusCode() == AndroidTestUnit.StatusCodes.IGNORED
        ) {
            ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.OK);
            ongoingTestUnit.setSuccess(true);
            performanceTestListener.testSuccess(ongoingTestUnit.getTitle());
        }
        ongoingTestUnit.setEndTimeMillis(System.currentTimeMillis());
        ongoingTestUnit.setRelEndTimeInVideo(ongoingTestUnit.getEndTimeMillis() - recordingStartTimeMillis);
    }

    @Override
    public void testRunFinished(Result result) {
        long elapsedTime = result.getRunTime();
        boolean wasSuccessful = result.wasSuccessful();

        if (wasSuccessful) {
            logEnter("testRunSuccessful", elapsedTime, Thread.currentThread().getName());
            testRun.addNewTimeTag("testRunSuccessful", System.currentTimeMillis() - recordingStartTimeMillis);
        } else {
            String errorMessage = result.getFailures().get(0).getMessage();
            logEnter("testRunFailed", errorMessage);
            testRun.addNewTimeTag("testRunFailed", System.currentTimeMillis() - recordingStartTimeMillis);
            testRun.setTestErrorMessage(errorMessage);
            if (errorMessage != null && errorMessage.toLowerCase(Locale.US).contains("process crash")) {
                if (testRun.getCrashStack() == null) {
                    testRun.setCrashStack(errorMessage);
                }
            }
        }

        logEnter("testRunEnded", elapsedTime, Thread.currentThread().getName());
        synchronized (this) {
            if (alreadyEnd) {
                return;
            }
            performanceTestListener.testRunFinished();
            testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
            testRun.onTestEnded();
            testRunDeviceOrchestrator.setRunningTestName(testRunDevice, null);
            if (!testTask.isDisableGifEncoder()) {
                testRunDeviceOrchestrator.stopGitEncoder(testRunDevice, agentManagementService.getScreenshotDir(), logger);
            }
            if (!testTask.isDisableRecording()) {
                String videoFilePath = testRunDeviceOrchestrator.stopScreenRecorder(testRunDevice, testRun.getResultFolder(), logger);
                testRun.setVideoPath(agentManagementService.getTestBaseRelPathInUrl(videoFilePath));
            }
            testRunDeviceOrchestrator.stopLogCollector(testRunDevice);
            alreadyEnd = true;
        }
    }

}
