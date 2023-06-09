// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.smart;

import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.agent.SmartTestParam;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.network.NetworkTestManagementService;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;

public class SmartRunner extends TestRunner {
    private static final int MAJOR_APPIUM_VERSION = 1;
    private static final int MINOR_APPIUM_VERSION = -1;
    private static final int MAJOR_PYTHON_VERSION = 3;
    private static final int MINOR_PYTHON_VERSION = 8;
    private final SmartTestUtil smartTestUtil;
    private long recordingStartTimeMillis;
    private int index;
    private String pkgName;
    private SmartTestParam smartTestParam;
    private TestRunDevice testRunDevice;
    private Logger logger;

    public SmartRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                       TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                       PerformanceTestManagementService performanceTestManagementService,
                       NetworkTestManagementService networkTestManagementService,
                       SmartTestUtil smartTestUtil) {
        super(agentManagementService, testTaskRunCallback, testRunDeviceOrchestrator, performanceTestManagementService, networkTestManagementService);
        this.smartTestUtil = smartTestUtil;
    }

    @Override
    protected List<EnvCapabilityRequirement> getEnvCapabilityRequirements() {
        return List.of(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.appium, MAJOR_APPIUM_VERSION, MINOR_APPIUM_VERSION),
                new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.python, MAJOR_PYTHON_VERSION, MINOR_PYTHON_VERSION));
    }

    @Override
    protected void run(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) throws Exception {

        this.testRunDevice = testRunDevice;
        testRun.setTotalCount(testTask.getDeviceTestCount());
        logger = testRun.getLogger();

        pkgName = testTask.getPkgName();

        /** start Record **/
        startTools(testRunDevice, testRun, testTask.getTimeOutSecond(), logger);

        /** run the test */
        logger.info("Start Smart test");
        checkTestTaskCancel(testTask);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        performanceTestManagementService.testRunStarted();

        /** init smart_test arg */
        //TODO choose model before starting test task
        smartTestParam = new SmartTestParam(testTask.getAppFile().getAbsolutePath(), testRunDevice.getDeviceInfo(), "0", "0",
                testTask.getMaxStepCount(), smartTestUtil.getFolderPath(), smartTestUtil.getStringFolderPath(), testRun.getResultFolder());

        for (int i = 1; i <= testTask.getDeviceTestCount(); i++) {
            checkTestTaskCancel(testTask);
            runSmartTestOnce(i, testRunDevice, testRun, logger);
        }
        testRunEnded(testRunDevice, testRun);
        /** set paths */
        String absoluteReportPath = testRun.getResultFolder().getAbsolutePath();
        testRun.setTestXmlReportPath(agentManagementService.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
        File gifFile = getGifFile();
        if (gifFile.exists() && gifFile.length() > 0) {
            testRun.setTestGifPath(agentManagementService.getTestBaseRelPathInUrl(gifFile));
        }

    }

    public void startTools(TestRunDevice testRunDevice, TestRun testRun, int maxTime, Logger logger) {
        logger.info("Start adb logcat collection");
        testRunDeviceOrchestrator.startLogCollector(testRunDevice, pkgName, testRun, testRun.getLogger());
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(testRunDevice.getLogPath())));

        logger.info("Start record screen");
        testRunDeviceOrchestrator.startScreenRecorder(testRunDevice, testRun.getResultFolder(), maxTime, testRun.getLogger());
        recordingStartTimeMillis = System.currentTimeMillis();
        final String initializing = "Initializing";
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, initializing);
        testRun.addNewTimeTag(initializing, 0);

        logger.info("Start gif frames collection");
        testRunDeviceOrchestrator.startGifEncoder(testRunDevice, testRun.getResultFolder(), pkgName + ".gif");
    }

    public File getGifFile() {
        return testRunDevice.getGifFile();
    }

    public void runSmartTestOnce(int i, TestRunDevice testRunDevice, TestRun testRun, Logger logger) {
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
        testRun.addNewTestUnit(ongoingSmartTest);

        testRun.addNewTimeTag(unitIndex + ". " + ongoingSmartTest.getTitle(), System.currentTimeMillis() - recordingStartTimeMillis);
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, ongoingSmartTest.getTitle());
        logger.info(ongoingSmartTest.getTitle());
        testRunDeviceOrchestrator.addGifFrameAsyncDelay(testRunDevice, agentManagementService.getScreenshotDir(), 1, logger);

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
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, null);
        testRun.addNewTimeTag(ongoingSmartTest.getTitle() + ".end",
                System.currentTimeMillis() - recordingStartTimeMillis);
        if (ongoingSmartTest.isSuccess()) {
            testRun.addNewTimeTag(ongoingSmartTest.getTitle() + ".res" + ":" + analysisRes,
                    System.currentTimeMillis() - recordingStartTimeMillis);
        }

    }

    public void testRunEnded(TestRunDevice testRunDevice, TestRun testRun) {
        performanceTestManagementService.testRunFinished();

        testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - recordingStartTimeMillis);
        testRun.onTestEnded();
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, null);
        testRunDeviceOrchestrator.stopGitEncoder(testRunDevice, agentManagementService.getScreenshotDir(), logger);
        testRunDeviceOrchestrator.stopScreenRecorder(testRunDevice, testRun.getResultFolder(), logger);
        testRunDeviceOrchestrator.stopLogCollector(testRunDevice);
    }

}
