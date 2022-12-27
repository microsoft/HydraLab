// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner.monkey;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.img.gif.AnimatedGifEncoder;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.logger.MultiLineNoCancelLoggingReceiver;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.LogUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AdbMonkeyRunner extends TestRunner {
    static final Logger classLogger = LoggerFactory.getLogger(AdbMonkeyRunner.class);
    private final AnimatedGifEncoder e = new AnimatedGifEncoder();
    final ADBOperateUtil adbOperateUtil;
    private LogCollector logCollector;
    private ScreenRecorder deviceScreenRecorder;
    private long recordingStartTimeMillis;
    private int index;
    private String pkgName;
    private File gifFile;
    private AndroidTestUnit ongoingMonkeyTest;

    public AdbMonkeyRunner(DeviceManager deviceManager, TestTaskRunCallback testTaskRunCallback, ADBOperateUtil adbOperateUtil) {
        super(deviceManager, testTaskRunCallback);
        this.adbOperateUtil = adbOperateUtil;
    }


    @Override
    protected void run(DeviceInfo deviceInfo, TestTask testTask, DeviceTestTask deviceTestTask) throws Exception {
        deviceTestTask.setTotalCount(1);
        Logger reportLogger = deviceTestTask.getLogger();

        pkgName = testTask.getPkgName();
        /** start Record **/
        logCollector = deviceManager.getLogCollector(deviceInfo, pkgName, deviceTestTask, reportLogger);
        deviceScreenRecorder = deviceManager.getScreenRecorder(deviceInfo, deviceTestTask.getDeviceTestResultFolder(), reportLogger);
        startRecording(deviceInfo, deviceTestTask, testTask.getTimeOutSecond(), reportLogger);

        /** run the test */
        reportLogger.info("Start monkey test");
        deviceTestTask.setTestStartTimeMillis(System.currentTimeMillis());
        checkTestTaskCancel(testTask);
        long checkTime = runMonkeyTestOnce(deviceInfo, deviceTestTask, reportLogger, testTask.getInstrumentationArgs(), testTask.getMaxStepCount());
        if (checkTime > 0) {
            String crashStack = deviceTestTask.getCrashStack();
            if (crashStack != null && !"".equals(crashStack)) {
                ongoingMonkeyTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
                ongoingMonkeyTest.setSuccess(false);
                ongoingMonkeyTest.setStack(crashStack);
                deviceTestTask.addNewTimeTagBeforeLast(ongoingMonkeyTest.getTitle() + ".fail", checkTime);
                deviceTestTask.oneMoreFailure();
            }
        }
        testRunEnded(deviceInfo, deviceTestTask);

        /** set paths */
        String absoluteReportPath = deviceTestTask.getDeviceTestResultFolder().getAbsolutePath();
        deviceTestTask.setTestXmlReportPath(deviceManager.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
        File gifFile = getGifFile();
        if (gifFile.exists() && gifFile.length() > 0) {
            deviceTestTask.setTestGifPath(deviceManager.getTestBaseRelPathInUrl(gifFile));
        }

    }


    public void startRecording(DeviceInfo deviceInfo, DeviceTestTask deviceTestTask, int maxTime, Logger logger) {
        startTools(deviceTestTask, logger);
        logger.info("Start record screen");
        deviceScreenRecorder.setupDevice();
        deviceScreenRecorder.startRecord(maxTime <= 0 ? 30 * 60 : maxTime);
        recordingStartTimeMillis = System.currentTimeMillis();
        final String initializing = "Initializing";
        deviceInfo.setRunningTestName(initializing);
        deviceTestTask.addNewTimeTag(initializing, 0);
    }

    private void startTools(DeviceTestTask deviceTestTask, Logger logger) {
        logger.info("Start gif frames collection");
        gifFile = new File(deviceTestTask.getDeviceTestResultFolder(), pkgName + ".gif");
        e.start(gifFile.getAbsolutePath());
        e.setDelay(1000);
        e.setRepeat(0);

        logger.info("Start adb logcat collection");
        String logcatFilePath = logCollector.start();
        deviceTestTask.setLogcatPath(deviceManager.getTestBaseRelPathInUrl(new File(logcatFilePath)));
    }

    public File getGifFile() {
        return gifFile;
    }

    public long runMonkeyTestOnce(DeviceInfo deviceInfo, DeviceTestTask deviceTestTask, Logger logger, Map<String, String> instrumentationArgs, int maxStepCount) {
        long checkTime = 0;
        final int unitIndex = ++index;
        String title = "Monkey_Test";

        ongoingMonkeyTest = new AndroidTestUnit();
        ongoingMonkeyTest.setNumtests(deviceTestTask.getTotalCount());
        ongoingMonkeyTest.setStartTimeMillis(System.currentTimeMillis());
        ongoingMonkeyTest.setRelStartTimeInVideo(ongoingMonkeyTest.getStartTimeMillis() - recordingStartTimeMillis);
        ongoingMonkeyTest.setCurrentIndexNum(unitIndex);
        ongoingMonkeyTest.setTestName(title);
        ongoingMonkeyTest.setTestedClass(pkgName);
        ongoingMonkeyTest.setDeviceTestResultId(deviceTestTask.getId());
        ongoingMonkeyTest.setTestTaskId(deviceTestTask.getTestTaskId());

        logger.info(ongoingMonkeyTest.getTitle());
        deviceManager.updateScreenshotImageAsyncDelay(deviceInfo, TimeUnit.SECONDS.toMillis(5), (imagePNGFile -> {
            if (imagePNGFile == null) {
                return;
            }
            if (!e.isStarted()) {
                return;
            }
            try {
                e.addFrame(ImgUtil.toBufferedImage(ImgUtil.scale(ImageIO.read(imagePNGFile), 0.3f)));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }), logger);
        //run monkey test
        deviceTestTask.addNewTimeTag(unitIndex + ". " + ongoingMonkeyTest.getTitle(), System.currentTimeMillis() - recordingStartTimeMillis);
        deviceInfo.setRunningTestName(ongoingMonkeyTest.getTitle());
        StringBuilder argString = new StringBuilder();
        if (instrumentationArgs != null && !instrumentationArgs.isEmpty()) {
            instrumentationArgs.forEach((k, v) -> argString.append(" ").append(v));
        }
        String commFormat;
        if (StringUtils.isBlank(argString.toString())) {
            commFormat = "monkey -p %s %d";
        } else {
            commFormat = "monkey -p %s %d" + argString;
        }
        try {
            String command = String.format(commFormat, pkgName, maxStepCount);
            // make sure pass is not printed
            logger.info(">> adb -s {} shell {}", deviceInfo.getSerialNum(), LogUtils.scrubSensitiveArgs(command));
            adbOperateUtil.executeShellCommandOnDevice(deviceInfo, command, new MultiLineNoCancelLoggingReceiver(logger), -1);
            checkTime = System.currentTimeMillis() - recordingStartTimeMillis;
            ongoingMonkeyTest.setStatusCode(AndroidTestUnit.StatusCodes.OK);
            ongoingMonkeyTest.setSuccess(true);
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);

            ongoingMonkeyTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
            ongoingMonkeyTest.setSuccess(false);
            ongoingMonkeyTest.setStack(e.toString());
            deviceTestTask.setSuccess(false);
            deviceTestTask.addNewTimeTagBeforeLast(ongoingMonkeyTest.getTitle() + ".fail", System.currentTimeMillis() - recordingStartTimeMillis);
            deviceTestTask.oneMoreFailure();
        }

        logger.info(ongoingMonkeyTest.getTitle() + ".end");
        ongoingMonkeyTest.setEndTimeMillis(System.currentTimeMillis());
        deviceInfo.setRunningTestName(null);
        deviceTestTask.addNewTestUnit(ongoingMonkeyTest);
        deviceTestTask.addNewTimeTag(ongoingMonkeyTest.getTitle() + ".end", System.currentTimeMillis() - recordingStartTimeMillis);
        return checkTime;
    }

    public void testRunEnded(DeviceInfo deviceInfo, DeviceTestTask deviceTestTask) {
        deviceTestTask.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
        deviceTestTask.onTestEnded();
        deviceInfo.setRunningTestName(null);
        releaseResource();
    }

    private void releaseResource() {
        e.finish();
        deviceScreenRecorder.finishRecording();
        logCollector.stopAndAnalyse();
    }

}
