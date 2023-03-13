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
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.device.TestDevice;
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
    private long recordingStartTimeMillis;
    private int index;
    private String pkgName;
    private File gifFile;
    private SmartTestParam smartTestParam;

    private TestDevice testDevice;

    public SmartRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                       PerformanceTestManagementService performanceTestManagementService,
                       SmartTestUtil smartTestUtil) {
        super(agentManagementService, testTaskRunCallback, performanceTestManagementService);
        this.smartTestUtil = smartTestUtil;
    }

    @Override
    protected void run(TestDevice testDevice, TestTask testTask, TestRun testRun) throws Exception {

        this.testDevice = testDevice;
        testRun.setTotalCount(testTask.getDeviceTestCount());
        Logger reportLogger = testRun.getLogger();

        pkgName = testTask.getPkgName();

        /** start Record **/
        startTools(testDevice, testRun, testTask.getTimeOutSecond(), reportLogger);

        /** run the test */
        reportLogger.info("Start Smart test");
        checkTestTaskCancel(testTask);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        performanceTestManagementService.testRunStarted();

        /** init smart_test arg */
        //TODO choose model before starting test task
        smartTestParam = new SmartTestParam(testTask.getAppFile().getAbsolutePath(), testDevice.getDeviceInfo(), "0", "0",
                testTask.getMaxStepCount(), smartTestUtil.getFolderPath(), smartTestUtil.getStringFolderPath());

        for (int i = 1; i <= testTask.getDeviceTestCount(); i++) {
            checkTestTaskCancel(testTask);
            runSmartTestOnce(i, testDevice, testRun, reportLogger);
        }
        testRunEnded(testDevice, testRun);
        /** set paths */
        String absoluteReportPath = testRun.getResultFolder().getAbsolutePath();
        testRun.setTestXmlReportPath(agentManagementService.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
        File gifFile = getGifFile();
        if (gifFile.exists() && gifFile.length() > 0) {
            testRun.setTestGifPath(agentManagementService.getTestBaseRelPathInUrl(gifFile));
        }

    }

    public void startTools(TestDevice testDevice, TestRun testRun, int maxTime, Logger logger) {
        logger.info("Start adb logcat collection");
        String logcatFilePath = testDevice.startLogCollector(pkgName, testRun, testRun.getLogger());
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(logcatFilePath)));

        logger.info("Start record screen");
        testDevice.startScreenRecorder(testRun.getResultFolder(), maxTime, testRun.getLogger());
        recordingStartTimeMillis = System.currentTimeMillis();
        final String initializing = "Initializing";
        testDevice.setRunningTestName(initializing);
        testRun.addNewTimeTag(initializing, 0);

        logger.info("Start gif frames collection");
        gifFile = new File(testRun.getResultFolder(), pkgName + ".gif");
        e.start(gifFile.getAbsolutePath());
        e.setDelay(1000);
        e.setRepeat(0);
    }

    public File getGifFile() {
        return gifFile;
    }

    public void runSmartTestOnce(int i, TestDevice testDevice, TestRun testRun, Logger logger) {
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

        testRun.addNewTimeTag(unitIndex + ". " + ongoingSmartTest.getTitle(), System.currentTimeMillis() - recordingStartTimeMillis);
        testDevice.setRunningTestName(ongoingSmartTest.getTitle());
        logger.info(ongoingSmartTest.getTitle());
        testDevice.updateScreenshotImageAsyncDelay(TimeUnit.SECONDS.toMillis(1), (imagePNGFile -> {
            if (imagePNGFile == null || !e.isStarted()) {
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
        testDevice.setRunningTestName(null);
        testRun.addNewTestUnit(ongoingSmartTest);
        testRun.addNewTimeTag(ongoingSmartTest.getTitle() + ".end",
                System.currentTimeMillis() - recordingStartTimeMillis);
        if (ongoingSmartTest.isSuccess()) {
            testRun.addNewTimeTag(ongoingSmartTest.getTitle() + ".res" + ":" + analysisRes,
                    System.currentTimeMillis() - recordingStartTimeMillis);
        }

    }

    public void testRunEnded(TestDevice testDevice, TestRun testRun) {
        performanceTestManagementService.testRunFinished();

        testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.onTestEnded();
        testDevice.setRunningTestName(null);
        releaseResource();
    }

    private void releaseResource() {
        e.finish();
        e.finish();
        testDevice.stopLogCollector();
        testDevice.stopScreenRecorder();
    }

}
