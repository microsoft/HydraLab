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
    // Todo: The following 2 methods are copied from AppiumCrossRunner. Need to be upgrade to support multiple device json test
    @Value("${app.registry.name}")
    String agentName;
    private int currentIndex = 0;

    @Override
    protected File runAndGetGif(File initialJsonFile, String unusedSuiteName, DeviceInfo deviceInfo, TestTask testTask,
                                DeviceTestTask deviceTestTask, File deviceTestResultFolder, Logger reportLogger) {
        pkgName = testTask.getPkgName();

        // Test start
        deviceScreenRecorder = deviceManager.getScreenRecorder(deviceInfo, deviceTestResultFolder, reportLogger);
        deviceScreenRecorder.setupDevice();
        deviceScreenRecorder.startRecord(testTask.getTimeOutSecond());
        long recordingStartTimeMillis = System.currentTimeMillis();

        logCollector = deviceManager.getLogCollector(deviceInfo, pkgName, deviceTestTask, reportLogger);
        logCollector.start();

        deviceTestTask.setTotalCount(testTask.testJsonFileList.size() + (initialJsonFile == null ? 0 : 1));
        deviceTestTask.setTestStartTimeMillis(System.currentTimeMillis());
        deviceTestTask.addNewTimeTag("testRunStarted", System.currentTimeMillis() - recordingStartTimeMillis);
        deviceInfo.setRunningTestName(pkgName.substring(pkgName.lastIndexOf('.') + 1) + ".testRunStarted");
        currentIndex = 0;

        gifFile = new File(deviceTestTask.getDeviceTestResultFolder(), pkgName + ".gif");
        e.start(gifFile.getAbsolutePath());
        e.setDelay(1000);
        e.setRepeat(0);

        if (initialJsonFile != null) {
            runT2CJsonTestCase(initialJsonFile, deviceInfo, deviceTestTask, reportLogger, recordingStartTimeMillis);
        }
        for (File jsonFile : testTask.testJsonFileList) {
            runT2CJsonTestCase(jsonFile, deviceInfo, deviceTestTask, reportLogger, recordingStartTimeMillis);
        }


        // Test finish
        reportLogger.info(pkgName + ".end");
        deviceTestTask.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
        deviceTestTask.onTestEnded();
        deviceInfo.setRunningTestName(null);
        releaseResource();
        return gifFile;
    }

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

    private void runT2CJsonTestCase(File jsonFile, DeviceInfo deviceInfo, DeviceTestTask deviceTestTask,
                                    Logger reportLogger, long recordingStartTimeMillis) {
        ongoingTest = new AndroidTestUnit();
        ongoingTest.setNumtests(deviceTestTask.getTotalCount());
        ongoingTest.setStartTimeMillis(System.currentTimeMillis());
        ongoingTest.setRelStartTimeInVideo(ongoingTest.getStartTimeMillis() - recordingStartTimeMillis);
        ongoingTest.setCurrentIndexNum(currentIndex++);
        ongoingTest.setTestName(jsonFile.getName());
        ongoingTest.setTestedClass(pkgName);
        ongoingTest.setDeviceTestResultId(deviceTestTask.getId());
        ongoingTest.setTestTaskId(deviceTestTask.getTestTaskId());

        reportLogger.info(ongoingTest.getTitle());

        deviceTestTask.addNewTimeTag(currentIndex + ". " + ongoingTest.getTitle(), System.currentTimeMillis() - recordingStartTimeMillis);
        deviceTestTask.addNewTestUnit(ongoingTest);

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
            deviceManager.runAppiumT2CTest(deviceInfo, jsonFile, reportLogger);
            ongoingTest.setStatusCode(AndroidTestUnit.StatusCodes.OK);
            ongoingTest.setSuccess(true);
        } catch (Exception e) {
            // Fail
            ongoingTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
            ongoingTest.setSuccess(false);
            ongoingTest.setStack(e.toString());
            deviceTestTask.addNewTimeTag(ongoingTest.getTitle() + ".fail", System.currentTimeMillis() - recordingStartTimeMillis);
            deviceTestTask.oneMoreFailure();
        }
        ongoingTest.setEndTimeMillis(System.currentTimeMillis());
        ongoingTest.setRelEndTimeInVideo(ongoingTest.getEndTimeMillis() - recordingStartTimeMillis);
        deviceTestTask.addNewTimeTag(ongoingTest.getTitle() + ".end", System.currentTimeMillis() - recordingStartTimeMillis);
    }

    private void releaseResource() {
        e.finish();
        deviceScreenRecorder.finishRecording();
        logCollector.stopAndAnalyse();
    }
}
