// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.t2c;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.img.gif.AnimatedGifEncoder;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.agent.runner.appium.AppiumRunner;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.device.TestDevice;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class T2CRunner extends AppiumRunner {

    private final AnimatedGifEncoder e = new AnimatedGifEncoder();
    private TestDevice testDevice;
    private String pkgName;
    String agentName;
    private int currentIndex = 0;

    public T2CRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                     PerformanceTestManagementService performanceTestManagementService, String agentName) {
        super(agentManagementService, testTaskRunCallback, performanceTestManagementService);
        this.agentName = agentName;
    }

    @Override
    protected File runAndGetGif(File initialJsonFile, String unusedSuiteName, TestDevice testDevice, TestTask testTask,
                                TestRun testRun, File deviceTestResultFolder, Logger reportLogger) {
        pkgName = testTask.getPkgName();
        this.testDevice = testDevice;
        // Test start
        testDevice.startScreenRecorder(deviceTestResultFolder, testTask.getTimeOutSecond(), reportLogger);
        long recordingStartTimeMillis = System.currentTimeMillis();

        testDevice.startLogCollector(pkgName, testRun, reportLogger);

        testRun.setTotalCount(testTask.testJsonFileList.size() + (initialJsonFile == null ? 0 : 1));
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        testRun.addNewTimeTag("testRunStarted", System.currentTimeMillis() - recordingStartTimeMillis);

        performanceTestManagementService.testRunStarted();

        testDevice.setRunningTestName(pkgName.substring(pkgName.lastIndexOf('.') + 1) + ".testRunStarted");
        currentIndex = 0;

        File gifFile = new File(testRun.getResultFolder(), pkgName + ".gif");
        e.start(gifFile.getAbsolutePath());
        e.setDelay(1000);
        e.setRepeat(0);

        if (initialJsonFile != null) {
            runT2CJsonTestCase(initialJsonFile, testDevice, testRun, reportLogger, recordingStartTimeMillis);
        }
        for (File jsonFile : testTask.testJsonFileList) {
            runT2CJsonTestCase(jsonFile, testDevice, testRun, reportLogger, recordingStartTimeMillis);
        }

        // Test finish
        reportLogger.info(pkgName + ".end");
        performanceTestManagementService.testRunFinished();
        testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.onTestEnded();
        testDevice.setRunningTestName(null);
        releaseResource();
        return gifFile;
    }

    @Override
    public TestRun createTestRun(TestDevice testDevice, TestTask testTask, Logger parentLogger) {
        TestRun testRun = super.createTestRun(testDevice, testTask, parentLogger);
        String deviceName =
                System.getProperties().getProperty("os.name") + "-" + agentName + "-" + testDevice.getName();
        testRun.setDeviceName(deviceName);
        return testRun;
    }

    private void runT2CJsonTestCase(File jsonFile, TestDevice testDevice, TestRun testRun,
                                    Logger reportLogger, long recordingStartTimeMillis) {
        AndroidTestUnit ongoingTest = new AndroidTestUnit();
        ongoingTest.setNumtests(testRun.getTotalCount());
        ongoingTest.setStartTimeMillis(System.currentTimeMillis());
        ongoingTest.setRelStartTimeInVideo(ongoingTest.getStartTimeMillis() - recordingStartTimeMillis);
        ongoingTest.setCurrentIndexNum(currentIndex++);
        ongoingTest.setTestName(jsonFile.getName());
        ongoingTest.setTestedClass(pkgName);
        ongoingTest.setDeviceTestResultId(testRun.getId());
        ongoingTest.setTestTaskId(testRun.getTestTaskId());

        reportLogger.info(ongoingTest.getTitle());

        testRun.addNewTimeTag(currentIndex + ". " + ongoingTest.getTitle(),
                System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.addNewTestUnit(ongoingTest);

        performanceTestManagementService.testStarted(ongoingTest.getTitle());

        testDevice.updateScreenshotImageAsyncDelay(TimeUnit.SECONDS.toMillis(5),
                (imagePNGFile -> {
                    if (imagePNGFile == null || !e.isStarted()) {
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
            testDevice.runAppiumT2CTest(jsonFile, reportLogger);
            performanceTestManagementService.testSuccess(ongoingTest.getTitle());
            ongoingTest.setStatusCode(AndroidTestUnit.StatusCodes.OK);
            ongoingTest.setSuccess(true);
        } catch (Exception e) {
            // Fail
            ongoingTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
            ongoingTest.setSuccess(false);
            ongoingTest.setStack(e.toString());
            performanceTestManagementService.testFailure(ongoingTest.getTitle());
            testRun.addNewTimeTag(ongoingTest.getTitle() + ".fail",
                    System.currentTimeMillis() - recordingStartTimeMillis);
            testRun.oneMoreFailure();
        }
        ongoingTest.setEndTimeMillis(System.currentTimeMillis());
        ongoingTest.setRelEndTimeInVideo(ongoingTest.getEndTimeMillis() - recordingStartTimeMillis);
        testRun.addNewTimeTag(ongoingTest.getTitle() + ".end",
                System.currentTimeMillis() - recordingStartTimeMillis);
    }

    private void releaseResource() {
        e.finish();
        testDevice.stopScreenRecorder();
        testDevice.stopLogCollector();
    }
}
