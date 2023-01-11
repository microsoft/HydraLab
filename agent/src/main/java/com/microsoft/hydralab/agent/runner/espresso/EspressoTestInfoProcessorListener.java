// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner.espresso;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.img.gif.AnimatedGifEncoder;
import cn.hutool.core.lang.Assert;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.Const;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EspressoTestInfoProcessorListener extends XmlTestRunListener {

    private final DeviceInfo deviceInfo;
    private final TestRun testRun;
    private final LogCollector adbLogcatCollector;
    private final ScreenRecorder adbDeviceScreenRecorder;
    private final Logger logger;
    private final AnimatedGifEncoder e = new AnimatedGifEncoder();
    private final String pkgName;
    private final DeviceManager deviceManager;
    ADBOperateUtil adbOperateUtil;
    private long recordingStartTimeMillis;
    private int index;
    private File gifFile;
    private boolean alreadyEnd = false;
    private AndroidTestUnit ongoingTestUnit;
    private int numTests;
    private int pid;
    private int addedFrameCount;

    public EspressoTestInfoProcessorListener(DeviceManager deviceManager, ADBOperateUtil adbOperateUtil, DeviceInfo deviceInfo, TestRun testRun, String pkgName) {
        this.deviceManager = deviceManager;
        this.adbOperateUtil = adbOperateUtil;
        this.deviceInfo = deviceInfo;
        this.testRun = testRun;
        this.logger = testRun.getLogger();
        this.pkgName = pkgName;
        adbLogcatCollector = deviceManager.getLogCollector(deviceInfo, pkgName, testRun, logger);
        adbDeviceScreenRecorder = deviceManager.getScreenRecorder(deviceInfo, testRun.getResultFolder(), logger);
        setReportDir(testRun.getResultFolder());
        try {
            setHostName(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public File getGifFile() {
        return gifFile;
    }

    public void startRecording(int maxTime) {
        logger.info("Start adb logcat collection");
        String logcatFilePath = adbLogcatCollector.start();
        testRun.setLogcatPath(deviceManager.getTestBaseRelPathInUrl(new File(logcatFilePath)));
        logger.info("Start record screen");
        adbDeviceScreenRecorder.setupDevice();
        adbDeviceScreenRecorder.startRecord(maxTime <= 0 ? 30 * 60 : maxTime);
        recordingStartTimeMillis = System.currentTimeMillis() + adbDeviceScreenRecorder.getPreSleepSeconds() * 1000L;
        final String initializing = "Initializing";
        deviceInfo.setRunningTestName(initializing);
        testRun.addNewTimeTag(initializing, 0);
    }

    @Override
    public void testRunStarted(String runName, int numTests) {
        logEnter("testRunStarted", runName, numTests);
        this.numTests = numTests;
        testRun.setTotalCount(numTests);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        testRun.addNewTimeTag("testRunStarted", System.currentTimeMillis() - recordingStartTimeMillis);
        deviceInfo.setRunningTestName(runName.substring(runName.lastIndexOf('.') + 1) + ".testRunStarted");
        super.testRunStarted(runName, numTests);
        logEnter(runName, numTests);
        startTools(runName);
    }


    private void startTools(String runName) {
        try {
            pid = adbOperateUtil.getPackagePid(deviceInfo, pkgName, logger);
            logger.info("{} is running test with pid {}", pkgName, pid);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        logger.info("Start gif frames collection");
        gifFile = new File(testRun.getResultFolder(), runName + ".gif");
        e.start(gifFile.getAbsolutePath());
        e.setDelay(1000);
        e.setRepeat(0);
    }

    private void logEnter(Object... args) {
        StringBuilder builder = new StringBuilder();
        for (Object arg : args) {
            builder.append(" >").append(arg);
        }
        logger.info("TestRunListener: {}", builder);
    }

    @Override
    public void testStarted(TestIdentifier test) {
        logEnter("testStarted", test);
        super.testStarted(test);
        ongoingTestUnit = new AndroidTestUnit();
        ongoingTestUnit.setNumtests(numTests);

        ongoingTestUnit.setStartTimeMillis(System.currentTimeMillis());
        ongoingTestUnit.setRelStartTimeInVideo(ongoingTestUnit.getStartTimeMillis() - recordingStartTimeMillis);

        final int unitIndex = ++index;
        ongoingTestUnit.setCurrentIndexNum(unitIndex);

        String method = test.getTestName();
        if (method == null) {
            ongoingTestUnit.setTestName("testInitialization");
        } else {
            ongoingTestUnit.setTestName(method);
        }
        ongoingTestUnit.setTestedClass(test.getClassName());

        testRun.addNewTimeTag(unitIndex + ". " + ongoingTestUnit.getTitle(), System.currentTimeMillis() - recordingStartTimeMillis);
        deviceInfo.setRunningTestName(ongoingTestUnit.getTitle());

        ongoingTestUnit.setDeviceTestResultId(testRun.getId());
        ongoingTestUnit.setTestTaskId(testRun.getTestTaskId());

        testRun.addNewTestUnit(ongoingTestUnit);

        deviceManager.updateScreenshotImageAsyncDelay(deviceInfo, TimeUnit.SECONDS.toMillis(5), (imagePNGFile -> {
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
    public void testFailed(TestIdentifier test, String trace) {
        logEnter("testFailed", test, trace);
        super.testFailed(test, trace);
        ongoingTestUnit.setStack(trace);
        ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
        testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".fail", System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.oneMoreFailure();
    }

    @Override
    public void testAssumptionFailure(TestIdentifier test, String trace) {
        logEnter("testAssumptionFailure", test, trace);
        super.testAssumptionFailure(test, trace);
        ongoingTestUnit.setStack(trace);
        testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".assumptionFail", System.currentTimeMillis() - recordingStartTimeMillis);
        ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.ASSUMPTION_FAILURE);
    }

    @Override
    public void testIgnored(TestIdentifier test) {
        logEnter("testIgnored", test);
        super.testIgnored(test);
        ongoingTestUnit.setStatusCode(AndroidTestUnit.StatusCodes.IGNORED);
    }

    @Override
    public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
        logEnter("testEnded", test, testMetrics);
        testRun.addNewTimeTag(ongoingTestUnit.getTitle() + ".end", System.currentTimeMillis() - recordingStartTimeMillis);
        super.testEnded(test, testMetrics);
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
    public void testRunFailed(String errorMessage) {
        logEnter("testRunFailed", errorMessage);
        testRun.addNewTimeTag("testRunFailed", System.currentTimeMillis() - recordingStartTimeMillis);
        Assert.isTrue(deviceInfo.isAlive(), Const.TaskResult.error_device_offline);
        super.testRunFailed(errorMessage);
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
        // releaseResource();
    }

    @Override
    public void testRunStopped(long elapsedTime) {
        logEnter("testRunStopped", elapsedTime);
        testRun.addNewTimeTag("testRunStopped", System.currentTimeMillis() - recordingStartTimeMillis);
        super.testRunStopped(elapsedTime);
        // releaseResource();
    }

    @Override
    public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
        logEnter("testRunEnded", elapsedTime, runMetrics, Thread.currentThread().getName());
        synchronized (this) {
            if (alreadyEnd) {
                return;
            }
            testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
            super.testRunEnded(elapsedTime, runMetrics);
            testRun.onTestEnded();
            deviceInfo.setRunningTestName(null);
            releaseResource();
            alreadyEnd = true;
        }
    }

    private void releaseResource() {
        e.finish();
        adbDeviceScreenRecorder.finishRecording();
        adbLogcatCollector.stopAndAnalyse();
    }

}
