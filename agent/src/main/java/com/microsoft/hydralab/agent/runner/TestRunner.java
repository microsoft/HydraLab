// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner;

import cn.hutool.core.lang.Assert;
import com.microsoft.hydralab.common.entity.agent.AgentFunctionAvailability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.common.DeviceAction;
import com.microsoft.hydralab.common.entity.common.Task;
import com.microsoft.hydralab.common.entity.common.TaskResult;
import com.microsoft.hydralab.common.entity.common.TestReport;
import com.microsoft.hydralab.common.entity.common.TestResult;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.screen.PhoneAppScreenRecorder;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.DateUtil;
import com.microsoft.hydralab.common.util.FlowUtil;
import com.microsoft.hydralab.common.util.LogUtils;
import com.microsoft.hydralab.common.util.ThreadPoolUtil;
import com.microsoft.hydralab.common.util.ThreadUtils;
import com.microsoft.hydralab.performance.InspectionStrategy;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class TestRunner implements TestRunEngine, TaskRunLifecycle<TestTask> {
    protected final Logger log = LoggerFactory.getLogger(TestRunner.class);
    protected final AgentManagementService agentManagementService;
    protected final TestTaskRunCallback testTaskRunCallback;
    protected final PerformanceTestManagementService performanceTestManagementService;
    protected final TestRunDeviceOrchestrator testRunDeviceOrchestrator;
    protected final XmlBuilder xmlBuilder = new XmlBuilder();

    public TestRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                      TestRunDeviceOrchestrator testRunDeviceOrchestrator, PerformanceTestManagementService performanceTestManagementService) {
        this.agentManagementService = agentManagementService;
        this.testTaskRunCallback = testTaskRunCallback;
        this.performanceTestManagementService = performanceTestManagementService;
        this.testRunDeviceOrchestrator = testRunDeviceOrchestrator;
        init();
    }

    void init() {
        agentManagementService.registerFunctionAvailability(getClass().getName(), AgentFunctionAvailability.AgentFunctionType.TEST_RUNNER, true, getEnvCapabilityRequirements());
    }

    protected abstract List<EnvCapabilityRequirement> getEnvCapabilityRequirements();

    @Override
    public TestReport run(Task task, TestRunDevice testRunDevice) {
        TestTask testTask = (TestTask) task;
        checkTestTaskCancel(testTask);
        TestRun testRun = initTestRun(testTask, testRunDevice);
        checkTestTaskCancel(testTask);

        TestReport testReport = null;
        TestResult testResult = null;
        try {
            setup(testTask, testRun);
            checkTestTaskCancel(testTask);
            execute(testTask, testRun);
            checkTestTaskCancel(testTask);
        } catch (Exception e) {
            testRun.getLogger().error(testRunDevice.getDeviceInfo().getSerialNum() + ": " + e.getMessage(), e);
            saveErrorSummary(testRun, e);
        } finally {
            testResult = analyze(testRun);
            testRun.setTaskResult(testResult);
            testReport = report(testRun, testResult);
            teardown(testTask, testRun);
            help(testRun, testResult);
        }
        return testReport;
    }

    @Override
    public TestRun initTestRun(TestTask testTask, TestRunDevice testRunDevice) {
        TestRun testRun = new TestRun(testRunDeviceOrchestrator.getSerialNum(testRunDevice), testRunDeviceOrchestrator.getName(testRunDevice), testTask.getId());
        testRun.setDevice(testRunDevice);
        File testRunResultFolder = new File(testTask.getResourceDir(), testRunDevice.getDeviceInfo().getSerialNum());
        Logger parentLogger = testRunDevice.getLogger();
        parentLogger.info("DeviceTestResultFolder {}", testRunResultFolder);
        if (!testRunResultFolder.exists()) {
            if (!testRunResultFolder.mkdirs()) {
                throw new RuntimeException("testRunResultFolder.mkdirs() failed: " + testRunResultFolder);
            }
        }

        testRun.setResultFolder(testRunResultFolder);
        Logger loggerForTestRun = createLoggerForTestRun(testRun, testTask.getPkgName(), parentLogger);
        testRun.setLogger(loggerForTestRun);
        testTask.addTestedDeviceResult(testRun);
        return testRun;
    }

    @Override
    public void execute(TestTask testTask, TestRun testRun) throws Exception {
        FutureTask<String> futureTask = new FutureTask<>(() -> {
            loadTestRunToCurrentThread(testRun);
            run(testRun.getDevice(), testTask, testRun);
            return null;
        });
        ThreadPoolUtil.TEST_EXECUTOR.execute(futureTask);
        try {
            if (testTask.getTimeOutSecond() > 0) {
                futureTask.get(testTask.getTimeOutSecond(), TimeUnit.SECONDS);
            } else {
                futureTask.get();
            }
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            futureTask.cancel(true);
            stopTest(testRun.getDevice());
            throw e;
        }
    }

    @Override
    public TestResult analyze(TestRun testRun) {
        TestResult testResult = new TestResult();
        testResult.setTotalCount(testRun.getTotalCount());
        testResult.setFailCount(testRun.getFailCount());
        testResult.setTaskId(testRun.getTestTaskId());
        testResult.setTaskRunId(testRun.getId());
        testResult.analysisState();
        return testResult;
    }

    @Override
    public TestReport report(TestRun testRun, TaskResult testResult) {
        return null;
    }

    @Override
    public void teardown(TestTask testTask, TestRun testRun) {
        TestRunDevice testRunDevice = testRun.getDevice();
        // stop performance test
        if (performanceTestManagementService != null) {
            try {
                performanceTestManagementService.testTearDown(testRun.getDevice(), testTask, testRun, agentManagementService.getRegistryServer());
            } catch (Exception e) {
                testRun.getLogger().error("Error in performance test tearDown", e);
            }
        }

        //execute actions
        if (testTask.getDeviceActions() != null) {
            testRun.getLogger().info("Start executing tearDown actions.");
            List<Exception> exceptions = testRunDeviceOrchestrator.doActions(testRunDevice, testRun.getLogger(),
                    testTask.getDeviceActions(), DeviceAction.When.TEAR_DOWN);
            if (exceptions.size() > 0) {
                testRun.getLogger().error("Execute actions failed when tearDown!", exceptions.get(0));
            }
        }

        try {
            //TODO: if the other test run resources are not released, release them here
            testRunDeviceOrchestrator.releaseScreenRecorder(testRunDevice, testRun.getLogger());
        } catch (Exception e) {
            testRun.getLogger().error("Error in release Screen Recorder", e);
        }

        testRunDeviceOrchestrator.testDeviceUnset(testRunDevice, testRun.getLogger());

        //generate xml report and upload files
        if (testRun.getTotalCount() > 0) {
            try {
                String absoluteReportPath = xmlBuilder.buildTestResultXml(testTask, testRun);
                testRun.setTestXmlReportPath(agentManagementService.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
            } catch (Exception e) {
                testRun.getLogger().error("Error in buildTestResultXml", e);
            }
        }
        if (testTaskRunCallback != null) {
            try {
                testTaskRunCallback.onOneDeviceComplete(testTask, testRunDevice, testRun.getLogger(), testRun);
            } catch (Exception e) {
                testRun.getLogger().error("Error in onOneDeviceComplete", e);
            }
        }
        testRun.getLogger().info("Start Close/finish resource");
        LogUtils.releaseLogger(testRun.getLogger());
    }

    @Override
    public void help(TestRun testRun, TaskResult testResult) {

    }

    /**
     * TODO Call {@link TestRunThreadContext#init(ITestRun)}
     * This method must be called in the test run execution thread.
     */
    private void loadTestRunToCurrentThread(TestRun testRun) {
        TestRunThreadContext.init(testRun);
    }

    private static void saveErrorSummary(TestRun testRun, Exception e) {
        String errorStr = e.getClass().getName() + ": " + e.getMessage();
        if (errorStr.length() > 255) {
            errorStr = errorStr.substring(0, 254);
        }
        testRun.setErrorInProcess(errorStr);
        testRun.onTestEnded();
    }

    protected void checkTestTaskCancel(TestTask testTask) {
        Assert.isFalse(testTask.isCanceled(), "Task {} is canceled", testTask.getId());
    }

    @Override
    public void setup(TestTask testTask, TestRun testRun) throws Exception {
        TestRunDevice testRunDevice = testRun.getDevice();
        // grant battery white list when testing android_client
        if (PhoneAppScreenRecorder.RECORD_PACKAGE_NAME.equals(testTask.getPkgName())) {
            Map<String, List<DeviceAction>> deviceActionsMap = testTask.getDeviceActions();
            List<DeviceAction> setUpDeviceActions = deviceActionsMap.getOrDefault(DeviceAction.When.SET_UP, new ArrayList<>());
            DeviceAction deviceAction1 = new DeviceAction(Const.OperatedDevice.ANDROID, "addToBatteryWhiteList");
            deviceAction1.setArgs(List.of(PhoneAppScreenRecorder.RECORD_PACKAGE_NAME));
            setUpDeviceActions.add(deviceAction1);
            deviceActionsMap.put(DeviceAction.When.SET_UP, setUpDeviceActions);
        }

        testRunDeviceOrchestrator.killAll(testRunDevice);
        // this key will be used to recover device status when lost the connection between agent and master
        testRunDeviceOrchestrator.addCurrentTask(testRunDevice, testTask);
        loadTestRunToCurrentThread(testRun);
        /* set up device */
        testRun.getLogger().info("Start setup device");
        testRunDeviceOrchestrator.testDeviceSetup(testRunDevice, testRun.getLogger());
        testRunDeviceOrchestrator.wakeUpDevice(testRunDevice, testRun.getLogger());
        testRunDeviceOrchestrator.unlockDevice(testRunDevice, testRun.getLogger());
        ThreadUtils.safeSleep(1000);
        checkTestTaskCancel(testTask);
        reInstallApp(testRunDevice, testTask, testRun.getLogger());
        reInstallTestApp(testRunDevice, testTask, testRun.getLogger());

        //execute actions
        if (testTask.getDeviceActions() != null) {
            testRun.getLogger().info("Start executing setUp actions.");
            List<Exception> exceptions = testRunDeviceOrchestrator.doActions(testRunDevice, testRun.getLogger(),
                    testTask.getDeviceActions(), DeviceAction.When.SET_UP);
            Assert.isTrue(exceptions.size() == 0, () -> exceptions.get(0));
        }

        testRun.getLogger().info("Start granting all package needed permissions device");
        testRunDeviceOrchestrator.grantAllTaskNeededPermissions(testRunDevice, testTask, testRun.getLogger());

        checkTestTaskCancel(testTask);
        if (!testTask.isDisableGifEncoder()) {
            testRunDeviceOrchestrator.getScreenShot(testRunDevice, agentManagementService.getScreenshotDir(), testRun.getLogger());
        }

        if (performanceTestManagementService != null && testTask.getInspectionStrategies() != null) {
            for (InspectionStrategy strategy : testTask.getInspectionStrategies()) {
                performanceTestManagementService.inspectWithStrategy(strategy);
            }
        }
    }

    protected abstract void run(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) throws Exception;

    protected void reInstallApp(TestRunDevice testRunDevice, TestTask testTask, Logger reportLogger) throws Exception {
        checkTestTaskCancel(testTask);
        if (testTask.getNeedUninstall()) {
            testRunDeviceOrchestrator.uninstallApp(testRunDevice, testTask.getPkgName(), reportLogger);
            ThreadUtils.safeSleep(3000);
        } else if (testTask.getNeedClearData()) {
            testRunDeviceOrchestrator.resetPackage(testRunDevice, testTask.getPkgName(), reportLogger);
        }
        if (testTask.getSkipInstall()) {
            return;
        }

        try {
            FlowUtil.retryAndSleepWhenFalse(3, 10, () -> testRunDeviceOrchestrator.installApp(testRunDevice, testTask.getAppFile().getAbsolutePath(), reportLogger));
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    protected void reInstallTestApp(TestRunDevice testRunDevice, TestTask testTask, Logger reportLogger)
            throws Exception {
        if (!shouldInstallTestPackageAsApp()) {
            return;
        }
        if (testTask.getTestAppFile() == null) {
            return;
        }
        if (StringUtils.isEmpty(testTask.getTestPkgName())) {
            return;
        }
        if (!testTask.getTestAppFile().exists()) {
            return;
        }
        if (testTask.getNeedUninstall()) {
            testRunDeviceOrchestrator.uninstallApp(testRunDevice, testTask.getTestPkgName(), reportLogger);
            // test package uninstall should be faster than app package removal.
            ThreadUtils.safeSleep(2000);
        }
        checkTestTaskCancel(testTask);
        try {
            FlowUtil.retryAndSleepWhenFalse(3, 10, () -> testRunDeviceOrchestrator.installApp(testRunDevice, testTask.getTestAppFile().getAbsolutePath(), reportLogger));
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    protected boolean shouldInstallTestPackageAsApp() {
        return false;
    }

    private Logger createLoggerForTestRun(TestRun testRun, String loggerNamePrefix, Logger parentLogger) {
        parentLogger.info("Start setup report child parentLogger");
        String dateInfo = DateUtil.fileNameDateFormat.format(new Date());
        File instrumentLogFile = new File(testRun.getResultFolder(), loggerNamePrefix + "_" + dateInfo + ".log");
        // make sure it's a child logger of the parentLogger
        String loggerName = parentLogger.getName() + ".test." + dateInfo;
        Logger reportLogger =
                LogUtils.getLoggerWithRollingFileAppender(loggerName, instrumentLogFile.getAbsolutePath(),
                        "%d %p -- %m%n");
        testRun.setInstrumentReportPath(agentManagementService.getTestBaseRelPathInUrl(instrumentLogFile));

        return reportLogger;
    }

    public void stopTest(TestRunDevice testRunDevice) {
        testRunDeviceOrchestrator.killAll(testRunDevice);
    }
}
