// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner.smart;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.img.gif.AnimatedGifEncoder;
import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.entity.agent.SmartTestParam;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.management.impl.IOSDeviceManager;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service("smartRunner")
public class SmartRunner extends TestRunner {
    private final AnimatedGifEncoder e = new AnimatedGifEncoder();
    @Resource
    SmartTestUtil smartTestUtil;
    private LogCollector logCollector;
    private ScreenRecorder deviceScreenRecorder;
    private long recordingStartTimeMillis;
    private int index;
    private String pkgName;
    private File gifFile;
    private SmartTestParam smartTestParam;
    private AndroidTestUnit ongoingSmartTest;

    @Override
    public void runTestOnDevice(TestTask testTask, DeviceInfo deviceInfo, Logger logger) {
        runSmartTest(deviceInfo, testTask, testTaskRunCallback, logger);
    }

    private void runSmartTest(DeviceInfo deviceInfo, TestTask testTask, TestTaskRunCallback testTaskRunCallback, Logger logger) {
        checkTestTaskCancel(testTask);
        logger.info("Start running tests {}, timeout {}s", testTask.getTestSuite(), testTask.getTimeOutSecond());

        DeviceTestTask deviceTestTask = initDeviceTestTask(deviceInfo, testTask, logger);
        deviceTestTask.setTotalCount(testTask.getDeviceTestCount());
        File deviceTestResultFolder = deviceTestTask.getDeviceTestResultFolder();
        testTask.addTestedDeviceResult(deviceTestTask);
        checkTestTaskCancel(testTask);

        Logger reportLogger = null;

        pkgName = testTask.getPkgName();

        try {
            reportLogger = deviceTestTask.getLogger();
            initDevice(deviceInfo, testTask, reportLogger);

            /** start Record **/
            logCollector = deviceManager.getLogCollector(deviceInfo, pkgName, deviceTestTask, logger);
            deviceScreenRecorder = deviceManager.getScreenRecorder(deviceInfo, deviceTestTask.getDeviceTestResultFolder(), reportLogger);
            startRecording(deviceInfo, deviceTestTask, testTask.getTimeOutSecond(), logger);

            /** run the test */
            reportLogger.info("Start Smart test");
            checkTestTaskCancel(testTask);
            deviceTestTask.setTestStartTimeMillis(System.currentTimeMillis());

            /** init smart_test arg */
            //TODO choose model before starting test task
            smartTestParam = new SmartTestParam(testTask.getAppFile().getAbsolutePath(), deviceInfo, "0", "0", testTask.getMaxStepCount(), smartTestUtil.getFolderPath(), smartTestUtil.getStringFolderPath());

            for (int i = 1; i <= testTask.getDeviceTestCount(); i++) {
                checkTestTaskCancel(testTask);
                runSmartTestOnce(i, deviceInfo, deviceTestTask, reportLogger);
            }
            testRunEnded(deviceInfo, deviceTestTask);
            /** set paths */
            String absoluteReportPath = deviceTestResultFolder.getAbsolutePath();
            deviceTestTask.setTestXmlReportPath(deviceManager.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
            File gifFile = getGifFile();
            if (gifFile.exists() && gifFile.length() > 0) {
                deviceTestTask.setTestGifPath(deviceManager.getTestBaseRelPathInUrl(gifFile));
            }

        } catch (Exception e) {
            if (reportLogger != null) {
                reportLogger.info(deviceInfo.getSerialNum() + ": " + e.getMessage(), e);
            } else {
                logger.info(deviceInfo.getSerialNum() + ": " + e.getMessage(), e);
            }
            String errorStr = e.getClass().getName() + ": " + e.getMessage();
            if (errorStr.length() > 255) {
                errorStr = errorStr.substring(0, 254);
            }
            deviceTestTask.setErrorInProcess(errorStr);
        } finally {
            afterTest(deviceInfo, testTask, deviceTestTask, testTaskRunCallback, reportLogger);
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

    public void runSmartTestOnce(int i, DeviceInfo deviceInfo, DeviceTestTask deviceTestTask, Logger logger) {
        final int unitIndex = ++index;
        String title = "Smart_Test(" + i + ")";

        ongoingSmartTest = new AndroidTestUnit();

        ongoingSmartTest.setNumtests(deviceTestTask.getTotalCount());
        ongoingSmartTest.setStartTimeMillis(System.currentTimeMillis());
        ongoingSmartTest.setRelStartTimeInVideo(ongoingSmartTest.getStartTimeMillis() - recordingStartTimeMillis);
        ongoingSmartTest.setCurrentIndexNum(unitIndex);
        ongoingSmartTest.setTestName(title);
        ongoingSmartTest.setTestedClass(pkgName);
        ongoingSmartTest.setDeviceTestResultId(deviceTestTask.getId());
        ongoingSmartTest.setTestTaskId(deviceTestTask.getTestTaskId());

        deviceTestTask.addNewTimeTag(unitIndex + ". " + ongoingSmartTest.getTitle(), System.currentTimeMillis() - recordingStartTimeMillis);
        deviceInfo.setRunningTestName(ongoingSmartTest.getTitle());
        logger.info(ongoingSmartTest.getTitle());
        deviceManager.updateScreenshotImageAsyncDelay(deviceInfo, TimeUnit.SECONDS.toMillis(1), (imagePNGFile -> {
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
        Boolean isSuccess = false;
        JSONObject res = new JSONObject();
        JSONObject analysisRes = new JSONObject();
        JSONArray crashStack = new JSONArray();
        try {
            String resString = smartTestUtil.runPYFunction(smartTestParam, logger);
            Assert.notEmpty(resString, "Run Smart Test Failed!");
            res = JSONObject.parseObject(resString);
            isSuccess = res.getBoolean(Const.SmartTestConfig.successTag);
            crashStack = res.getJSONArray(Const.SmartTestConfig.appExpTag);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.put(Const.SmartTestConfig.taskExpTag, e.getMessage());
        }
        if (!isSuccess) {
            ongoingSmartTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
            ongoingSmartTest.setSuccess(false);
            ongoingSmartTest.setStack(res.getString(Const.SmartTestConfig.taskExpTag));
            deviceTestTask.addNewTimeTag(ongoingSmartTest.getTitle() + ".fail", System.currentTimeMillis() - recordingStartTimeMillis);
            deviceTestTask.oneMoreFailure();
        } else if (crashStack != null && crashStack.size() > 0) {
            ongoingSmartTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
            ongoingSmartTest.setSuccess(false);
            ongoingSmartTest.setStack(crashStack.toJSONString());
            deviceTestTask.addNewTimeTag(ongoingSmartTest.getTitle() + ".fail", System.currentTimeMillis() - recordingStartTimeMillis);
            deviceTestTask.oneMoreFailure();
        } else {
            analysisRes = smartTestUtil.analysisRes(res);
            ongoingSmartTest.setStatusCode(AndroidTestUnit.StatusCodes.OK);
            ongoingSmartTest.setSuccess(true);
        }
        ongoingSmartTest.setEndTimeMillis(System.currentTimeMillis());
        logger.info(ongoingSmartTest.getTitle() + ".end");
        deviceInfo.setRunningTestName(null);
        deviceTestTask.addNewTestUnit(ongoingSmartTest);
        deviceTestTask.addNewTimeTag(ongoingSmartTest.getTitle() + ".end", System.currentTimeMillis() - recordingStartTimeMillis);
        if (ongoingSmartTest.isSuccess()) {
            deviceTestTask.addNewTimeTag(ongoingSmartTest.getTitle() + ".res" + ":" + analysisRes, System.currentTimeMillis() - recordingStartTimeMillis);
        }

    }

    public void testRunEnded(DeviceInfo deviceInfo, DeviceTestTask deviceTestTask) {

        deviceTestTask.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
        deviceTestTask.onTestEnded();
        deviceInfo.setRunningTestName(null);
        releaseResource();
    }

    private void releaseResource() {
        e.finish();
        e.finish();
        deviceScreenRecorder.finishRecording();
        logCollector.stopAndAnalyse();
    }

    @Override
    public void reInstallApp(DeviceInfo deviceInfo, TestTask testTask, Logger reportLogger) throws Exception {
        if (testTask.getRequireReinstall() || deviceManager instanceof IOSDeviceManager) {
            deviceManager.uninstallApp(deviceInfo, testTask.getPkgName(), reportLogger);
            deviceManager.safeSleep(1000);
        } else if (testTask.getRequireClearData()) {
            deviceManager.resetPackage(deviceInfo, testTask.getPkgName(), reportLogger);
        }
        checkTestTaskCancel(testTask);

        deviceManager.installApp(deviceInfo, testTask.getAppFile().getAbsolutePath(), reportLogger);
    }
}
