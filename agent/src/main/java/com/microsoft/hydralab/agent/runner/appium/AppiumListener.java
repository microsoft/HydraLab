// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.appium;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.img.gif.AnimatedGifEncoder;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.device.TestDeviceManager;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.performance.PerformanceTestListener;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AppiumListener extends RunListener {
    private final DeviceInfo deviceInfo;
    private final TestRun testRun;
    private final LogCollector logcatCollector;
    private final ScreenRecorder deviceScreenRecorder;
    private final Logger logger;
    private final AnimatedGifEncoder e = new AnimatedGifEncoder();
    private final String pkgName;
    TestDeviceManager testDeviceManager;
    AgentManagementService agentManagementService;
    private final PerformanceTestListener performanceTestListener;
    private long recordingStartTimeMillis;
    private int index;
    private File gifFile;
    private boolean alreadyEnd = false;
    private AndroidTestUnit ongoingTestUnit;
    private int numTests;
    private int pid;
    private int addedFrameCount;
    private String currentTestName = "";
    private int currentTestIndex = 0;

    public AppiumListener(AgentManagementService agentManagementService, DeviceInfo deviceInfo, TestRun testRun,
                          String pkgName, PerformanceTestListener performanceTestListener, Logger logger) {
        this.agentManagementService = agentManagementService;
        this.testDeviceManager = deviceInfo.getTestDeviceManager();
        this.deviceInfo = deviceInfo;
        this.testRun = testRun;
        this.logger = logger;
        this.pkgName = pkgName;
        this.performanceTestListener = performanceTestListener;
        logcatCollector = testDeviceManager.getLogCollector(deviceInfo, pkgName, testRun, logger);
        deviceScreenRecorder = testDeviceManager.getScreenRecorder(deviceInfo, testRun.getResultFolder(), logger);
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
//        try {
//            pid = adbDeviceManager.getPackagePid(deviceInfo, pkgName, logger);
//            logger.info("{} is running test with pid {}", pkgName, pid);
//        } catch (Exception exception) {
//            exception.printStackTrace();
//        }
        logger.info("Start gif frames collection");
        gifFile = new File(testRun.getResultFolder(), pkgName + ".gif");
        e.start(gifFile.getAbsolutePath());
        e.setDelay(1000);
        e.setRepeat(0);

        logger.info("Start logcat collection");
        String logcatFilePath = logcatCollector.start();
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(logcatFilePath)));
    }

    @Override
    public void testRunStarted(Description description) {
        String runName = description.getChildren().get(0).getDisplayName();
        int testCount = description.getChildren().get(0).testCount();
        logEnter("testRunStarted", runName, testCount);
        this.numTests = description.testCount();
        testRun.setTotalCount(testCount);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        testRun.addNewTimeTag("testRunStarted", System.currentTimeMillis() - recordingStartTimeMillis);
        deviceInfo.setRunningTestName(runName.substring(runName.lastIndexOf('.') + 1) + ".testRunStarted");
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
        deviceInfo.setRunningTestName(ongoingTestUnit.getTitle());

        ongoingTestUnit.setDeviceTestResultId(testRun.getId());
        ongoingTestUnit.setTestTaskId(testRun.getTestTaskId());

        testRun.addNewTestUnit(ongoingTestUnit);

        testDeviceManager.updateScreenshotImageAsyncDelay(deviceInfo, TimeUnit.SECONDS.toMillis(15),
                (imagePNGFile -> {
                    if (imagePNGFile == null || !e.isStarted()) {
                        return;
                    }
                    try {
                        e.addFrame(ImgUtil.toBufferedImage(ImgUtil.scale(ImageIO.read(imagePNGFile), 0.3f)));
                        addedFrameCount++;
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }), logger);
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
            if (e.isStarted() && addedFrameCount < 2) {
                testDeviceManager.updateScreenshotImageAsyncDelay(deviceInfo, TimeUnit.SECONDS.toMillis(0),
                        (imagePNGFile -> {
                            try {
                                e.addFrame(ImgUtil.toBufferedImage(ImgUtil.scale(ImageIO.read(imagePNGFile), 0.3f)));
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }), logger);
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
            deviceInfo.setRunningTestName(null);
            releaseResource();
            alreadyEnd = true;
        }
    }

    private void releaseResource() {
        e.finish();
        deviceScreenRecorder.finishRecording();
        logcatCollector.stopAndAnalyse();
        logger.info("Record Finished");
    }
}
