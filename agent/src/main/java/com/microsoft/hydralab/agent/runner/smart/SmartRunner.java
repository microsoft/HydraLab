// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.smart;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.img.gif.AnimatedGifEncoder;
import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.entity.agent.SmartTestParam;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SmartRunner extends TestRunner {
    private final AnimatedGifEncoder e = new AnimatedGifEncoder();
    private final SmartTestUtil smartTestUtil;
    private LogCollector logCollector;
    private ScreenRecorder deviceScreenRecorder;
    private long recordingStartTimeMillis;
    private int index;
    private String pkgName;
    private File gifFile;
    private SmartTestParam smartTestParam;

    public SmartRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                       PerformanceTestManagementService performanceTestManagementService,
                       SmartTestUtil smartTestUtil) {
        super(agentManagementService, testTaskRunCallback, performanceTestManagementService);
        this.smartTestUtil = smartTestUtil;
    }

    @Override
    protected void run(DeviceInfo deviceInfo, TestTask testTask, TestRun testRun) throws Exception {

        testRun.setTotalCount(testTask.getDeviceTestCount());
        Logger reportLogger = testRun.getLogger();

        pkgName = testTask.getPkgName();

        /** start Record **/
        logCollector = testDeviceManager.getLogCollector(deviceInfo, pkgName, testRun, reportLogger);
        deviceScreenRecorder =
                testDeviceManager.getScreenRecorder(deviceInfo, testRun.getResultFolder(), reportLogger);
        startRecording(deviceInfo, testRun, testTask.getTimeOutSecond(), reportLogger);

        /** run the test */
        reportLogger.info("Start Smart test");
        checkTestTaskCancel(testTask);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        performanceTestManagementService.testRunStarted();

        /** init smart_test arg */
        //TODO choose model before starting test task
        smartTestParam = new SmartTestParam(testTask.getAppFile().getAbsolutePath(), deviceInfo, "0", "0",
                testTask.getMaxStepCount(), smartTestUtil.getFolderPath(), smartTestUtil.getStringFolderPath());

        for (int i = 1; i <= testTask.getDeviceTestCount(); i++) {
            checkTestTaskCancel(testTask);
            runSmartTestOnce(i, deviceInfo, testRun, reportLogger);
        }
        testRunEnded(deviceInfo, testRun);
        /** set paths */
        String absoluteReportPath = testRun.getResultFolder().getAbsolutePath();
        testRun.setTestXmlReportPath(agentManagementService.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
        File gifFile = getGifFile();
        if (gifFile.exists() && gifFile.length() > 0) {
            testRun.setTestGifPath(agentManagementService.getTestBaseRelPathInUrl(gifFile));
        }

    }

    public void startRecording(DeviceInfo deviceInfo, TestRun testRun, int maxTime, Logger logger) {
        startTools(testRun, logger);
        logger.info("Start record screen");
        deviceScreenRecorder.setupDevice();
        deviceScreenRecorder.startRecord(maxTime <= 0 ? 30 * 60 : maxTime);
        recordingStartTimeMillis = System.currentTimeMillis();
        final String initializing = "Initializing";
        deviceInfo.setRunningTestName(initializing);
        testRun.addNewTimeTag(initializing, 0);
    }

    private void startTools(TestRun testRun, Logger logger) {
        logger.info("Start gif frames collection");
        gifFile = new File(testRun.getResultFolder(), pkgName + ".gif");
        e.start(gifFile.getAbsolutePath());
        e.setDelay(1000);
        e.setRepeat(0);

        logger.info("Start adb logcat collection");
        String logcatFilePath = logCollector.start();
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(logcatFilePath)));
    }

    public File getGifFile() {
        return gifFile;
    }

    public void runSmartTestOnce(int i, DeviceInfo deviceInfo, TestRun testRun, Logger logger) {
        final int unitIndex = ++index;
        String title = "Smart_Test(" + i + ")";

        AndroidTestUnit ongoingSmartTest = new AndroidTestUnit();

        ongoingSmartTest.setNumtests(testRun.getTotalCount());
        ongoingSmartTest.setStartTimeMillis(System.currentTimeMillis());
        ongoingSmartTest.setRelStartTimeInVideo(ongoingSmartTest.getStartTimeMillis() - recordingStartTimeMillis);
        ongoingSmartTest.setCurrentIndexNum(unitIndex);
        ongoingSmartTest.setTestName(title);
        ongoingSmartTest.setTestedClass(pkgName);
        ongoingSmartTest.setDeviceTestResultId(testRun.getId());
        ongoingSmartTest.setTestTaskId(testRun.getTestTaskId());

        testRun.addNewTimeTag(unitIndex + ". " + ongoingSmartTest.getTitle(),
                System.currentTimeMillis() - recordingStartTimeMillis);
        deviceInfo.setRunningTestName(ongoingSmartTest.getTitle());
        logger.info(ongoingSmartTest.getTitle());
        testDeviceManager.updateScreenshotImageAsyncDelay(deviceInfo, TimeUnit.SECONDS.toMillis(1),
                (imagePNGFile -> {
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

        performanceTestManagementService.testStarted(ongoingSmartTest.getTitle());

        Boolean isSuccess = false;
        JSONObject res = new JSONObject();
        JSONObject analysisRes = new JSONObject();
        JSONArray crashStack = new JSONArray();
        try {
            String resString = smartTestUtil.runPYFunction(smartTestParam, logger);
            Assert.notEmpty(resString, "Run Smart Test Failed!");
            res = JSONObject.parseObject(resString);
            isSuccess = res.getBoolean(Const.SmartTestConfig.SUCCESS_TAG);
            crashStack = res.getJSONArray(Const.SmartTestConfig.APP_EXP_TAG);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.put(Const.SmartTestConfig.TASK_EXP_TAG, e.getMessage());
        }
        if (!isSuccess) {
            ongoingSmartTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
            ongoingSmartTest.setSuccess(false);
            ongoingSmartTest.setStack(res.getString(Const.SmartTestConfig.TASK_EXP_TAG));
            performanceTestManagementService.testFailure(ongoingSmartTest.getTitle());
            testRun.addNewTimeTag(ongoingSmartTest.getTitle() + ".fail",
                    System.currentTimeMillis() - recordingStartTimeMillis);
            testRun.oneMoreFailure();
        } else if (crashStack != null && crashStack.size() > 0) {
            ongoingSmartTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
            ongoingSmartTest.setSuccess(false);
            ongoingSmartTest.setStack(crashStack.toJSONString());
            performanceTestManagementService.testFailure(ongoingSmartTest.getTitle());
            testRun.addNewTimeTag(ongoingSmartTest.getTitle() + ".fail",
                    System.currentTimeMillis() - recordingStartTimeMillis);
            testRun.oneMoreFailure();
        } else {
            analysisRes = smartTestUtil.analysisRes(res);
            ongoingSmartTest.setStatusCode(AndroidTestUnit.StatusCodes.OK);
            ongoingSmartTest.setSuccess(true);
            performanceTestManagementService.testSuccess(ongoingSmartTest.getTitle());
        }
        ongoingSmartTest.setEndTimeMillis(System.currentTimeMillis());
        logger.info(ongoingSmartTest.getTitle() + ".end");
        deviceInfo.setRunningTestName(null);
        testRun.addNewTestUnit(ongoingSmartTest);
        testRun.addNewTimeTag(ongoingSmartTest.getTitle() + ".end",
                System.currentTimeMillis() - recordingStartTimeMillis);
        if (ongoingSmartTest.isSuccess()) {
            testRun.addNewTimeTag(ongoingSmartTest.getTitle() + ".res" + ":" + analysisRes,
                    System.currentTimeMillis() - recordingStartTimeMillis);
        }

    }

    public void testRunEnded(DeviceInfo deviceInfo, TestRun testRun) {
        performanceTestManagementService.testRunFinished();

        testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.onTestEnded();
        deviceInfo.setRunningTestName(null);
        releaseResource();
    }

    private void releaseResource() {
        e.finish();
        e.finish();
        deviceScreenRecorder.finishRecording();
        logCollector.stopAndAnalyse();
    }

}
