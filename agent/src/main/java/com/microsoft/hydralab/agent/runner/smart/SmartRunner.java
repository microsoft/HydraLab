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
import com.microsoft.hydralab.common.entity.agent.LLMProperties;
import com.microsoft.hydralab.common.entity.agent.SmartTestParam;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.util.Const;
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
    private final LLMProperties llmProperties;

    public SmartRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                       TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                       PerformanceTestManagementService performanceTestManagementService,
                       SmartTestUtil smartTestUtil, LLMProperties llmProperties) {
        super(agentManagementService, testTaskRunCallback, testRunDeviceOrchestrator, performanceTestManagementService);
        this.smartTestUtil = smartTestUtil;
        this.llmProperties = llmProperties;
    }

    @Override
    protected List<EnvCapabilityRequirement> getEnvCapabilityRequirements() {
        return List.of(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.appium, MAJOR_APPIUM_VERSION, MINOR_APPIUM_VERSION),
                new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.python, MAJOR_PYTHON_VERSION, MINOR_PYTHON_VERSION));
    }

    @Override
    protected void run(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) throws Exception {
        testRun.setTotalCount(testTask.getDeviceTestCount());
        Logger logger = testRun.getLogger();

        /* start Record */
        startTools(testRunDevice, testTask, testRun, logger);

        /* run the test */
        logger.info("Start Smart test");
        checkTestTaskCancel(testTask);
        performanceTestManagementService.testRunStarted();

        /* init smart_test arg */
        //TODO choose model before starting test task
        SmartTestParam smartTestParam = new SmartTestParam(testTask.getAppFile().getAbsolutePath(), testRunDevice.getDeviceInfo(), "0", "0",
                testTask.getMaxStepCount(), smartTestUtil.getFolderPath(), smartTestUtil.getStringFolderPath(), testRun.getResultFolder(), llmProperties);

        for (int i = 1; i <= testTask.getDeviceTestCount(); i++) {
            checkTestTaskCancel(testTask);
            runSmartTestOnce(i, testRunDevice, testTask, testRun, smartTestParam, logger);
        }
        testRunEnded(testRunDevice, testTask, testRun);
        /* set paths */
        String absoluteReportPath = testRun.getResultFolder().getAbsolutePath();
        testRun.setTestXmlReportPath(agentManagementService.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
        File gifFile = testRunDevice.getGifFile();
        if (gifFile.exists() && gifFile.length() > 0) {
            testRun.setTestGifPath(agentManagementService.getTestBaseRelPathInUrl(gifFile));
        }

    }

    public void startTools(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun, Logger logger) {
        logger.info("Start adb logcat collection");
        testRunDeviceOrchestrator.startLogCollector(testRunDevice, testTask.getPkgName(), testRun, testRun.getLogger());
        testRun.setLogcatPath(agentManagementService.getTestBaseRelPathInUrl(new File(testRunDevice.getLogPath())));

        logger.info("Start record screen");
        if (!testTask.isDisableRecording()) {
            testRunDeviceOrchestrator.startScreenRecorder(testRunDevice, testRun.getResultFolder(), testTask.getTimeOutSecond(), testRun.getLogger());
        }

        final String initializing = "Initializing";
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, initializing);
        testRun.addNewTimeTag(initializing, 0);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());

        if (!testTask.isDisableGifEncoder()) {
            logger.info("Start gif frames collection");
            testRunDeviceOrchestrator.startGifEncoder(testRunDevice, testRun.getResultFolder(), testTask.getPkgName() + ".gif");
        }
    }

    public void runSmartTestOnce(int i, TestRunDevice testRunDevice, TestTask testTask, TestRun testRun, SmartTestParam smartTestParam, Logger logger) {
        final int unitIndex = i + 1;
        String title = "Smart_Test(" + i + ")";

        AndroidTestUnit ongoingSmartTest = new AndroidTestUnit();

        ongoingSmartTest.setNumtests(testRun.getTotalCount());
        ongoingSmartTest.setStartTimeMillis(System.currentTimeMillis());
        ongoingSmartTest.setRelStartTimeInVideo(ongoingSmartTest.getStartTimeMillis() - testRun.getTestStartTimeMillis());
        ongoingSmartTest.setCurrentIndexNum(unitIndex);
        ongoingSmartTest.setTestName(title);
        ongoingSmartTest.setTestedClass(testTask.getPkgName());
        ongoingSmartTest.setDeviceTestResultId(testRun.getId());
        ongoingSmartTest.setTestTaskId(testRun.getTestTaskId());
        testRun.addNewTestUnit(ongoingSmartTest);

        testRun.addNewTimeTag(unitIndex + ". " + ongoingSmartTest.getTitle(), System.currentTimeMillis() - testRun.getTestStartTimeMillis());
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
            logger.info("Python script result string: " + resString);
            res = JSONObject.parseObject(resString);
            isSuccess = res.getBoolean(Const.SmartTestConfig.SUCCESS_TAG);
            crashStack = res.getJSONArray(Const.SmartTestConfig.APP_EXP_TAG);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.put(Const.SmartTestConfig.TASK_EXP_TAG, e.getMessage());
        }
        if (!isSuccess) {
            ongoingSmartTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
            logger.info("Run smart test failed, isSuccess: " + isSuccess);
            ongoingSmartTest.setSuccess(false);
            ongoingSmartTest.setStack(res.getString(Const.SmartTestConfig.TASK_EXP_TAG));
            performanceTestManagementService.testFailure(ongoingSmartTest.getTitle());
            testRun.addNewTimeTag(ongoingSmartTest.getTitle() + ".fail",
                    System.currentTimeMillis() - testRun.getTestStartTimeMillis());
            testRun.oneMoreFailure();
        } else if (crashStack != null && crashStack.size() > 0) {
            ongoingSmartTest.setStatusCode(AndroidTestUnit.StatusCodes.FAILURE);
            logger.info("Crash stack is found:" + crashStack.toJSONString());
            ongoingSmartTest.setSuccess(false);
            ongoingSmartTest.setStack(crashStack.toJSONString());
            performanceTestManagementService.testFailure(ongoingSmartTest.getTitle());
            testRun.addNewTimeTag(ongoingSmartTest.getTitle() + ".fail",
                    System.currentTimeMillis() - testRun.getTestStartTimeMillis());
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
                System.currentTimeMillis() - testRun.getTestStartTimeMillis());
        if (ongoingSmartTest.isSuccess()) {
            testRun.addNewTimeTag(ongoingSmartTest.getTitle() + ".res" + ":" + analysisRes,
                    System.currentTimeMillis() - testRun.getTestStartTimeMillis());
        }

    }

    public void testRunEnded(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) {
        performanceTestManagementService.testRunFinished();

        testRun.addNewTimeTag("testRunEnded", System.currentTimeMillis() - testRun.getTestStartTimeMillis());
        testRun.onTestEnded();
        testRunDeviceOrchestrator.setRunningTestName(testRunDevice, null);
        if (!testTask.isDisableGifEncoder()) {
            testRunDeviceOrchestrator.stopGitEncoder(testRunDevice, agentManagementService.getScreenshotDir(), testRun.getLogger());
        }
        if (!testTask.isDisableRecording()) {
            testRunDeviceOrchestrator.stopScreenRecorder(testRunDevice, testRun.getResultFolder(), testRun.getLogger());
        }
        testRunDeviceOrchestrator.stopLogCollector(testRunDevice);
    }

}
