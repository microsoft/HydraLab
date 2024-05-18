// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.analysis;

import cn.hutool.core.lang.Assert;
import com.microsoft.hydralab.agent.runner.TaskRunLifecycle;
import com.microsoft.hydralab.agent.runner.TestRunEngine;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.service.TestTaskEngineService;
import com.microsoft.hydralab.common.entity.agent.AgentFunctionAvailability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.common.AnalysisTask;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.entity.common.Task;
import com.microsoft.hydralab.common.entity.common.TaskResult;
import com.microsoft.hydralab.common.entity.common.TestReport;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.util.DateUtil;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.common.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author zhoule
 * @date 11/20/2023
 */

public abstract class AnalysisRunner implements TaskRunLifecycle<AnalysisTask>, TestRunEngine {
    protected final Logger log = LoggerFactory.getLogger(TestRunner.class);
    protected final AgentManagementService agentManagementService;
    protected final TestTaskEngineService testTaskEngineService;

    public AnalysisRunner(AgentManagementService agentManagementService, TestTaskEngineService testTaskEngineService, boolean isEnabled) {
        this.agentManagementService = agentManagementService;
        this.testTaskEngineService = testTaskEngineService;
        init(isEnabled);
    }

    void init(boolean isEnabled) {
        agentManagementService.registerFunctionAvailability(getFunctionName(), AgentFunctionAvailability.AgentFunctionType.ANALYSIS_RUNNER, isEnabled,
                getEnvCapabilityRequirements());
    }

    protected abstract String getFunctionName();

    protected abstract List<EnvCapabilityRequirement> getEnvCapabilityRequirements();

    @Override
    public TestRun initTestRun(AnalysisTask analysisTask, TestRunDevice testRunDevice) {
        DeviceInfo deviceInfo = testRunDevice.getDeviceInfo();
        TestRun testRun = new TestRun(deviceInfo.getSerialNum(), deviceInfo.getName(), analysisTask.getId());
        testRun.setDevice(testRunDevice);
        testRun.setTestTaskId(analysisTask.getId());
        File testRunResultFolder = new File(analysisTask.getResourceDir(), testRunDevice.getDeviceInfo().getSerialNum());
        String file = agentManagementService.getDeviceLogBaseDir().getAbsolutePath() + "/" + testRunDevice.getDeviceInfo().getName() +
                "/device_control.log";
        Logger parentLogger = LogUtils.getLoggerWithRollingFileAppender(LogCollector.LOGGER_PREFIX + testRunDevice.getDeviceInfo().getSerialNum(), file,
                "%d %logger{0} %p [%t] - %m%n");
        parentLogger.info("DeviceTestResultFolder {}", testRunResultFolder);
        if (!testRunResultFolder.exists()) {
            if (!testRunResultFolder.mkdirs()) {
                throw new RuntimeException("testRunResultFolder.mkdirs() failed: " + testRunResultFolder);
            }
        }

        testRun.setResultFolder(testRunResultFolder);
        Logger loggerForTestRun = createLoggerForTestRun(testRun, analysisTask.getPkgName(), parentLogger);
        testRun.setLogger(loggerForTestRun);
        analysisTask.addTestedDeviceResult(testRun);
        return testRun;
    }

    private Logger createLoggerForTestRun(TestRun testRun, String loggerNamePrefix, Logger parentLogger) {
        parentLogger.info("Start setup report child parentLogger");
        String dateInfo = DateUtil.fileNameDateFormat.format(new Date());
        File agentLogFile = new File(testRun.getResultFolder(), loggerNamePrefix + "_" + dateInfo + ".log");
        // make sure it's a child logger of the parentLogger
        String loggerName = parentLogger.getName() + ".test." + dateInfo;
        Logger reportLogger =
                LogUtils.getLoggerWithRollingFileAppender(loggerName, agentLogFile.getAbsolutePath(),
                        "%d %p -- %m%n");
        testRun.setInstrumentReportPath(agentManagementService.getTestBaseRelPathInUrl(agentLogFile));

        return reportLogger;
    }

    @Override
    public void setup(AnalysisTask analysisTask, TestRun testRun) throws Exception {

    }

    @Override
    public void execute(AnalysisTask analysisTask, TestRun testRun) throws Exception {
    }

    @Override
    public TaskResult analyze(TestRun testRun) {
        return testRun.getTaskResult();
    }

    @Override
    public TestReport report(TestRun testRun, TaskResult testResult) {
        return null;
    }

    @Override
    public void teardown(AnalysisTask testTask, TestRun testRun) {
        TestRunDevice testRunDevice = testRun.getDevice();
        log.info("onOneDeviceComplete: {}", testRunDevice.getDeviceInfo().getSerialNum());
        File deviceTestResultFolder = testRun.getResultFolder();

        File[] files = deviceTestResultFolder.listFiles();
        List<StorageFileInfo> attachments = new ArrayList<>();
        Assert.notNull(files, "should have result file to upload");
        for (File file : files) {
            if (!file.isDirectory()) {
                attachments.add(testTaskEngineService.saveFileToBlob(file, deviceTestResultFolder, testRun.getLogger()));
            } else if (file.listFiles().length > 0) {
                File zipFile = FileUtil.zipFile(file.getAbsolutePath(),
                        deviceTestResultFolder + "/" + file.getName() + ".zip");
                attachments.add(testTaskEngineService.saveFileToBlob(zipFile, deviceTestResultFolder, testRun.getLogger()));
            }
        }
        testRun.setAttachments(attachments);
        testRun.processAndSaveDeviceTestResultBlobUrl();
        testRun.getLogger().info("Start Close/finish resource");
        LogUtils.releaseLogger(testRun.getLogger());
    }

    @Override
    public void help(TestRun testRun, TaskResult testResult) {
    }

    protected void checkTestTaskCancel(Task task) {
        Assert.isFalse(task.isCanceled(), "Task {} is canceled", task.getId());
    }

    protected void saveErrorSummary(TestRun testRun, Exception e) {
        String errorStr = e.getClass().getName() + ": " + e.getMessage();
        if (errorStr.length() > 255) {
            errorStr = errorStr.substring(0, 254);
        }
        testRun.setErrorInProcess(errorStr);
        testRun.onTestEnded();
    }

    @Override
    public TestReport run(Task task, TestRunDevice testRunDevice) {
        AnalysisTask analysisTask = (AnalysisTask) task;
        checkTestTaskCancel(analysisTask);
        TestRun testRun = initTestRun(analysisTask, testRunDevice);
        checkTestTaskCancel(analysisTask);

        TestReport testReport = null;
        TaskResult testResult = null;
        try {
            setup(analysisTask, testRun);
            execute(analysisTask, testRun);
            checkTestTaskCancel(analysisTask);

            testResult = analyze(testRun);
            checkTestTaskCancel(analysisTask);

            testReport = report(testRun, testResult);
            checkTestTaskCancel(analysisTask);
        } catch (Exception e) {
            testRun.getLogger().error(e.getMessage(), e);
            saveErrorSummary(testRun, e);
        } finally {
            teardown(analysisTask, testRun);
            help(testRun, testResult);
        }
        return testReport;
    }
}
