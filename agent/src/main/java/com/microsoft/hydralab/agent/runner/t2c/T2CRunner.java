// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner.t2c;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.img.gif.AnimatedGifEncoder;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.agent.runner.appium.AppiumRunner;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.performace.PerformanceManager;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.performance.PerformanceInspectionService;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class T2CRunner extends AppiumRunner {

    private final AnimatedGifEncoder e = new AnimatedGifEncoder();
    private LogCollector logCollector;
    private ScreenRecorder deviceScreenRecorder;
    private String pkgName;
    String agentName;
    private int currentIndex = 0;

    public T2CRunner(DeviceManager deviceManager, TestTaskRunCallback testTaskRunCallback, String agentName, PerformanceManager performanceManager) {
        super(deviceManager, testTaskRunCallback, performanceManager);
        this.agentName = agentName;
    }

    @Override
    protected File runAndGetGif(File initialJsonFile, String unusedSuiteName, DeviceInfo deviceInfo, TestTask testTask,
                                DeviceTestTask deviceTestTask, File deviceTestResultFolder,
                                PerformanceInspectionService performanceInspectionService, Logger reportLogger) {
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

        File gifFile = new File(deviceTestTask.getDeviceTestResultFolder(), pkgName + ".gif");
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
    public DeviceTestTask buildDeviceTestTask(DeviceInfo deviceInfo, TestTask testTask, Logger parentLogger) {
        DeviceTestTask deviceTestTask = super.buildDeviceTestTask(deviceInfo, testTask, parentLogger);
        String deviceName = System.getProperties().getProperty("os.name") + "-" + agentName + "-" + deviceInfo.getName();
        deviceTestTask.setDeviceName(deviceName);
        return deviceTestTask;
    }

    private void runT2CJsonTestCase(File jsonFile, DeviceInfo deviceInfo, DeviceTestTask deviceTestTask,
                                    Logger reportLogger, long recordingStartTimeMillis) {
        AndroidTestUnit ongoingTest = new AndroidTestUnit();
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
