// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner.appium;


import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.img.gif.AnimatedGifEncoder;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Junit5Listener extends SummaryGeneratingListener {
    private final DeviceManager deviceManager;
    private final DeviceInfo deviceInfo;
    private final TestRun testRun;
    private final LogCollector logcatCollector;
    private final ScreenRecorder deviceScreenRecorder;
    private final Logger logger;
    private final AnimatedGifEncoder e = new AnimatedGifEncoder();
    private final String pkgName;
    private long recordingStartTimeMillis;
    private int index;
    private File gifFile;
    private boolean alreadyEnd = false;
    private AndroidTestUnit ongoingTestUnit;
    private int pid;
    private int addedFrameCount;
    private String currentTestName = "";
    private int currentTestIndex = 0;

    public Junit5Listener(DeviceManager deviceManager, DeviceInfo deviceInfo, TestRun testRun, String pkgName, Logger logger) {
        this.deviceManager = deviceManager;
        this.deviceInfo = deviceInfo;
        this.testRun = testRun;
        this.logger = logger;
        this.pkgName = pkgName;
        logcatCollector = deviceManager.getLogCollector(deviceInfo, pkgName, testRun, logger);
        deviceScreenRecorder = deviceManager.getScreenRecorder(deviceInfo, testRun.getResultFolder(), logger);
    }

    public File getGifFile() {
        return gifFile;
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
        startTools();
        logger.info("Start record screen");
        deviceScreenRecorder.setupDevice();
        deviceScreenRecorder.startRecord(maxTime <= 0 ? 30 * 60 : maxTime);
        recordingStartTimeMillis = System.currentTimeMillis();
        final String initializing = "Initializing";
        deviceInfo.setRunningTestName(initializing);
        testRun.addNewTimeTag(initializing, 0);
    }

    private void startTools() {
        logger.info("Start gif frames collection");
        gifFile = new File(testRun.getResultFolder(), pkgName + ".gif");
        e.start(gifFile.getAbsolutePath());
        e.setDelay(1000);
        e.setRepeat(0);

        logger.info("Start logcat collection");
        String logcatFilePath = logcatCollector.start();
        testRun.setLogcatPath(deviceManager.getTestBaseRelPathInUrl(new File(logcatFilePath)));
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
        deviceInfo.setRunningTestName(runName.substring(runName.lastIndexOf('.') + 1) + ".testRunStarted");
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
            if (e.isStarted() && addedFrameCount < 2) {
                try {
                    File imagePNGFile = deviceManager.getScreenShot(deviceInfo, logger);
                    e.addFrame(ImgUtil.toBufferedImage(ImgUtil.scale(ImageIO.read(imagePNGFile), 0.3f)));
                } catch (Exception exception) {
                    logger.error(exception.getMessage(), e);
                }
            }

        }

        logEnter("testRunEnded", elapsedTime, Thread.currentThread().getName());
        synchronized (this) {
            if (alreadyEnd) {
                return;
            }
            testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
            testRun.onTestEnded();
            deviceInfo.setRunningTestName(null);
            releaseResource();
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

        testRun.addNewTimeTag(unitIndex + ". " + ongoingTestUnit.getTitle(), System.currentTimeMillis() - recordingStartTimeMillis);
        deviceInfo.setRunningTestName(ongoingTestUnit.getTitle());

        ongoingTestUnit.setDeviceTestResultId(testRun.getId());
        ongoingTestUnit.setTestTaskId(testRun.getTestTaskId());

        testRun.addNewTestUnit(ongoingTestUnit);

        deviceManager.updateScreenshotImageAsyncDelay(deviceInfo, TimeUnit.SECONDS.toMillis(15), (imagePNGFile -> {
            if (imagePNGFile == null) {
                return;
            }
            if (!e.isStarted()) {
                return;
            }
            try {
                e.addFrame(ImgUtil.toBufferedImage(ImgUtil.scale(ImageIO.read(imagePNGFile), 0.3f)));
                addedFrameCount++;
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }), logger);
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
            testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".fail", System.currentTimeMillis() - recordingStartTimeMillis);
            testRun.oneMoreFailure();
        }
        ongoingTestUnit.setEndTimeMillis(System.currentTimeMillis());
        ongoingTestUnit.setRelEndTimeInVideo(ongoingTestUnit.getEndTimeMillis() - recordingStartTimeMillis);
        testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".end", System.currentTimeMillis() - recordingStartTimeMillis);
    }

    private void releaseResource() {
        e.finish();
        deviceScreenRecorder.finishRecording();
        logcatCollector.stopAndAnalyse();
    }

    public String getTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        throwable.printStackTrace(writer);
        return stringWriter.toString();
    }
}
