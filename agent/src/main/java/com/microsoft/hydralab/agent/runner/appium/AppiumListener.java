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
    private final TestRun deviceTestResult;
    private final LogCollector logcatCollector;
    private final ScreenRecorder deviceScreenRecorder;
    private final Logger logger;
    private final AnimatedGifEncoder e = new AnimatedGifEncoder();
    private final String pkgName;
    DeviceManager deviceManager;
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


    public AppiumListener(DeviceManager deviceManager, DeviceInfo deviceInfo, TestRun deviceTestResult, String pkgName, Logger logger) {
        this.deviceManager = deviceManager;
        this.deviceInfo = deviceInfo;
        this.deviceTestResult = deviceTestResult;
        this.logger = logger;
        this.pkgName = pkgName;
        logcatCollector = deviceManager.getLogCollector(deviceInfo, pkgName, deviceTestResult, logger);
        deviceScreenRecorder = deviceManager.getScreenRecorder(deviceInfo, deviceTestResult.getTestRunResultFolder(), logger);
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
        deviceTestResult.addNewTimeTag(initializing, 0);
    }

    private void startTools() {
//        try {
//            pid = adbDeviceManager.getPackagePid(deviceInfo, pkgName, logger);
//            logger.info("{} is running test with pid {}", pkgName, pid);
//        } catch (Exception exception) {
//            exception.printStackTrace();
//        }
        logger.info("Start gif frames collection");
        gifFile = new File(deviceTestResult.getTestRunResultFolder(), pkgName + ".gif");
        e.start(gifFile.getAbsolutePath());
        e.setDelay(1000);
        e.setRepeat(0);

        logger.info("Start logcat collection");
        String logcatFilePath = logcatCollector.start();
        deviceTestResult.setLogcatPath(deviceManager.getTestBaseRelPathInUrl(new File(logcatFilePath)));
    }

    @Override
    public void testRunStarted(Description description) {
        String runName = description.getChildren().get(0).getDisplayName();
        int testCount = description.getChildren().get(0).testCount();
        logEnter("testRunStarted", runName, testCount);
        this.numTests = description.testCount();
        deviceTestResult.setTotalCount(testCount);
        deviceTestResult.setTestStartTimeMillis(System.currentTimeMillis());
        deviceTestResult.addNewTimeTag("testRunStarted", System.currentTimeMillis() - recordingStartTimeMillis);
        deviceInfo.setRunningTestName(runName.substring(runName.lastIndexOf('.') + 1) + ".testRunStarted");
        logEnter(runName, description.testCount());
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

        deviceTestResult.addNewTimeTag(unitIndex + ". " + ongoingTestUnit.getTitle(), System.currentTimeMillis() - recordingStartTimeMillis);
        deviceInfo.setRunningTestName(ongoingTestUnit.getTitle());

        ongoingTestUnit.setDeviceTestResultId(deviceTestResult.getId());
        ongoingTestUnit.setTestTaskId(deviceTestResult.getTestTaskId());

        deviceTestResult.addNewTestUnit(ongoingTestUnit);

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
    public void testFailure(Failure failure) {
        String testDisplayName = failure.getDescription().getDisplayName();
        logEnter("testFailed", testDisplayName, failure.getTrace());
        ongoingTestUnit.setStack(failure.getTrace());
        ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
        deviceTestResult.addNewTimeTag(ongoingTestUnit.getTitle() + ".fail", System.currentTimeMillis() - recordingStartTimeMillis);
        deviceTestResult.oneMoreFailure();
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        String testDisplayName = failure.getDescription().getDisplayName();
        logEnter("testAssumptionFailure", testDisplayName, failure.getTrace());
        ongoingTestUnit.setStack(failure.getTrace());
        ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
        deviceTestResult.addNewTimeTag(ongoingTestUnit.getTitle() + ".assumptionFail", System.currentTimeMillis() - recordingStartTimeMillis);
        deviceTestResult.oneMoreFailure();
    }

    @Override
    public void testIgnored(Description description) {
        logEnter("testIgnored", description.getDisplayName());
        ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.IGNORED);
    }

    @Override
    public void testFinished(Description description) {
        logEnter("testEnded", description.getDisplayName());
        deviceTestResult.addNewTimeTag(ongoingTestUnit.getTitle() + ".end", System.currentTimeMillis() - recordingStartTimeMillis);
        if (ongoingTestUnit.getStatusCode() == 0
                || ongoingTestUnit.getStatusCode() == AndroidTestUnit.StatusCodes.ASSUMPTION_FAILURE
                || ongoingTestUnit.getStatusCode() == AndroidTestUnit.StatusCodes.IGNORED
        ) {
            ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.OK);
            ongoingTestUnit.setSuccess(true);
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
            deviceTestResult.addNewTimeTag("testRunSuccessful", System.currentTimeMillis() - recordingStartTimeMillis);
        } else {
            String errorMessage = result.getFailures().get(0).getMessage();
            logEnter("testRunFailed", errorMessage);
            deviceTestResult.addNewTimeTag("testRunFailed", System.currentTimeMillis() - recordingStartTimeMillis);
            deviceTestResult.setTestErrorMessage(errorMessage);
            if (errorMessage != null && errorMessage.toLowerCase(Locale.US).contains("process crash")) {
                if (deviceTestResult.getCrashStack() == null) {
                    deviceTestResult.setCrashStack(errorMessage);
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
            deviceTestResult.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
            deviceTestResult.onTestEnded();
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
