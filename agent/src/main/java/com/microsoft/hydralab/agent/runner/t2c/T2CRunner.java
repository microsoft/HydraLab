// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner.t2c;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.img.gif.AnimatedGifEncoder;
import com.microsoft.hydralab.agent.runner.appium.AppiumRunner;
import com.microsoft.hydralab.common.entity.center.TestTaskSpec;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class T2CRunner extends AppiumRunner {

    private final AnimatedGifEncoder e = new AnimatedGifEncoder();
    private LogCollector logCollector;
    private ScreenRecorder deviceScreenRecorder;
    private String pkgName;
    private File gifFile;
    private AndroidTestUnit ongoingTest;
    @Override
    protected File runAndGetGif(File jsonFile, String unusedSuiteName, DeviceInfo deviceInfo, TestTask testTask, DeviceTestTask deviceTestTask, File deviceTestResultFolder, Logger reportLogger) {
        pkgName = testTask.getPkgName();

        long recordingStartTimeMillis = System.currentTimeMillis();
        deviceScreenRecorder = deviceManager.getScreenRecorder(deviceInfo, deviceTestResultFolder, reportLogger);
        deviceScreenRecorder.startRecord(testTask.getTimeOutSecond());

        logCollector = deviceManager.getLogCollector(deviceInfo, pkgName, deviceTestTask, reportLogger);
        logCollector.start();
        deviceTestTask.setTotalCount(1);

        ongoingTest = new AndroidTestUnit();
        ongoingTest.setNumtests(deviceTestTask.getTotalCount());
        ongoingTest.setStartTimeMillis(System.currentTimeMillis());
        ongoingTest.setRelStartTimeInVideo(ongoingTest.getStartTimeMillis() - recordingStartTimeMillis);
        ongoingTest.setCurrentIndexNum(1);
        ongoingTest.setTestName("Taps_to_Cases");
        ongoingTest.setTestedClass(pkgName);
        ongoingTest.setDeviceTestResultId(deviceTestTask.getId());
        ongoingTest.setTestTaskId(deviceTestTask.getTestTaskId());

        reportLogger.info(ongoingTest.getTitle());

        deviceTestTask.addNewTimeTag(1 + ". " + ongoingTest.getTitle(), System.currentTimeMillis() - recordingStartTimeMillis);
        deviceInfo.setRunningTestName(ongoingTest.getTitle());
        gifFile = new File(deviceTestTask.getDeviceTestResultFolder(), pkgName + ".gif");
        e.start(gifFile.getAbsolutePath());
        e.setDelay(1000);
        e.setRepeat(0);
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
        }), reportLogger);

        // Run Test
        try {
            deviceTestTask.setTestStartTimeMillis(System.currentTimeMillis());
            deviceManager.runAppiumT2CTest(deviceInfo, jsonFile, reportLogger);
            ongoingTest.setStatusCode(AndroidTestUnit.StatusCodes.OK);
            ongoingTest.setSuccess(true);
            deviceTestTask.setSuccess(true);
        } catch (Exception e) {
            // Fail
            ongoingTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
            ongoingTest.setSuccess(false);
            ongoingTest.setStack(e.toString());
            deviceTestTask.setSuccess(false);
            deviceTestTask.addNewTimeTagBeforeLast(ongoingTest.getTitle() + ".fail", System.currentTimeMillis() - recordingStartTimeMillis);
            deviceTestTask.oneMoreFailure();
        }
        ongoingTest.setEndTimeMillis(System.currentTimeMillis());
        deviceScreenRecorder.finishRecording();
        logCollector.stopAndAnalyse();
        // Test finish
        reportLogger.info(ongoingTest.getTitle() + ".end");

        deviceInfo.setRunningTestName(null);
        deviceTestTask.addNewTestUnit(ongoingTest);
        deviceTestTask.addNewTimeTag(ongoingTest.getTitle() + ".end", System.currentTimeMillis() - recordingStartTimeMillis);
        deviceTestTask.onTestEnded();
        return gifFile;
    }

    // Todo: The following 2 methods are copied from AppiumCrossRunner. Need to be upgrade to support multiple device json test
    @Value("${app.registry.name}")
    String agentName;
    @Override
    protected Set<DeviceInfo> chooseDevices(TestTaskSpec testTaskSpec) {
        Set<DeviceInfo> allActiveConnectedDevice = deviceManager.getActiveDeviceList(log);
        log.info("Choosing devices from {}", allActiveConnectedDevice.size());
        Assert.notNull(allActiveConnectedDevice, "No connected device!");
        Assert.isTrue(allActiveConnectedDevice.size() == 1, "No connected device!");

        return allActiveConnectedDevice;
    }

    @Override
    public DeviceTestTask initDeviceTestTask(DeviceInfo deviceInfo, TestTask testTask, Logger logger) {
        DeviceTestTask deviceTestTask = super.initDeviceTestTask(deviceInfo, testTask, logger);
        String deviceName = System.getProperties().getProperty("os.name") + "-" + agentName + "-" + deviceInfo.getName();
        deviceTestTask.setDeviceName(deviceName);
        return deviceTestTask;
    }
}
