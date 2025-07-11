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
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.slf4j.Logger;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

public class Junit5Listener extends SummaryGeneratingListener {
    private final AgentManagementService agentManagementService;
    private final PerformanceTestListener performanceTestListener;
    private final TestRunDevice testRunDevice;
    private final TestRun testRun;
    private final TestTask testTask;
    private final Logger logger;
    private final String pkgName;
    private long recordingStartTimeMillis;
    private boolean alreadyEnd = false;
    private AndroidTestUnit ongoingTestUnit;
    private String currentTestName = "";
    private int currentTestIndex = 0;
    private TestRunDeviceOrchestrator testRunDeviceOrchestrator;

    public Junit5Listener(AgentManagementService agentManagementService, TestRunDevice testRunDevice, TestRun testRun,
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
    public void testPlanExecutionStarted(TestPlan testPlan) {
        super.testPlanExecutionStarted(testPlan);
        String runName = pkgName;
        int testCount = (int) getSummary().getTestsFoundCount();
        logEnter("testRunStarted", runName, testCount);
        testRun.setTotalCount(testCount);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        testRun.addNewTimeTag("testRunStarted", System.currentTimeMillis() - recordingStartTimeMillis);
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, runName.substring(runName.lastIndexOf('.') + 1) + ".testRunStarted");
        performanceTestListener.testRunStarted();
        logEnter(runName, testCount);
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        super.testPlanExecutionFinished(testPlan);
        long elapsedTime = getSummary().getTimeFinished() - getSummary().getTimeStarted();
        boolean wasSuccessful = getSummary().getTestsFailedCount() == 0;

        if (wasSuccessful) {
            logEnter("testRunSuccessful", elapsedTime, Thread.currentThread().getName());
            testRun.addNewTimeTag("testRunSuccessful", System.currentTimeMillis() - recordingStartTimeMillis);
        } else {
            String errorMessage = getSummary().getFailures().get(0).getException().getMessage();
            logEnter("testRunFailed", errorMessage);
            testRun.addNewTimeTag("testRunFailed", System.currentTimeMillis() - recordingStartTimeMillis);
            testRun.setTestErrorMessage(errorMessage);
            if (errorMessage != null && errorMessage.toLowerCase(Locale.US).contains("process crash")) {
                if (testRun.getCrashStack() == null) {
                    testRun.setCrashStack(errorMessage);
                }
            }
        }
        performanceTestListener.testRunFinished();
        logEnter("testRunEnded", elapsedTime, Thread.currentThread().getName());
        synchronized (this) {
            if (alreadyEnd) {
                return;
            }
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

    @Override
    public void dynamicTestRegistered(TestIdentifier testIdentifier) {
        super.dynamicTestRegistered(testIdentifier);
        System.out.println(testIdentifier.getDisplayName());
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        super.executionSkipped(testIdentifier, reason);
        logEnter("testIgnored", testIdentifier.getDisplayName());
        ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.IGNORED);
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        super.executionStarted(testIdentifier);
        if (!"TEST".equals(testIdentifier.getType().name())) {
            return;
        }
        MethodSource testSource = (MethodSource) testIdentifier.getSource().get();
        String testName = testSource.getMethodName() == null ? "testInitialization" : testSource.getMethodName();
        String testClassName = testSource.getClassName();
        String testDisplayName = testIdentifier.getDisplayName();
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
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        super.executionFinished(testIdentifier, testExecutionResult);
        if (!"TEST".equals(testIdentifier.getType().name())) {
            return;
        }
        if ("SUCCESSFUL".equals(testExecutionResult.getStatus().name())) {
            logEnter("testEnded", testIdentifier.getDisplayName());
            ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.OK);
            ongoingTestUnit.setSuccess(true);
            performanceTestListener.testSuccess(ongoingTestUnit.getTitle());
        } else {

            Throwable throwable;
            if (testExecutionResult.getThrowable().isPresent()) {
                throwable = testExecutionResult.getThrowable().get();
            } else {
                throwable = new Throwable(ongoingTestUnit.getTitle() + ".fail");
            }
            String testDisplayName = testIdentifier.getDisplayName();
            logEnter("testFailed", testDisplayName, getTrace(throwable));
            ongoingTestUnit.setStack(getTrace(throwable));
            ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
            testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".fail",
                    System.currentTimeMillis() - recordingStartTimeMillis);
            testRun.oneMoreFailure();
            performanceTestListener.testFailure(ongoingTestUnit.getTestName());
        }
        ongoingTestUnit.setEndTimeMillis(System.currentTimeMillis());
        ongoingTestUnit.setRelEndTimeInVideo(ongoingTestUnit.getEndTimeMillis() - recordingStartTimeMillis);
        testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".end",
                System.currentTimeMillis() - recordingStartTimeMillis);
    }

    public String getTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        throwable.printStackTrace(writer);
        return stringWriter.toString();
    }
}
