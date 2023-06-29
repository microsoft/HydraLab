// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner;

import cn.hutool.core.lang.Assert;
import com.microsoft.hydralab.common.entity.agent.AgentFunctionAvailability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.common.DeviceAction;
import com.microsoft.hydralab.common.entity.common.TestReport;
import com.microsoft.hydralab.common.entity.common.TestResult;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
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
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class TestRunner implements TestRunEngine, TestRunLifecycle {
    protected final Logger log = LoggerFactory.getLogger(TestRunner.class);
    protected final AgentManagementService agentManagementService;
    protected final TestTaskRunCallback testTaskRunCallback;
    protected final PerformanceTestManagementService performanceTestManagementService;
    protected final TestRunDeviceOrchestrator testRunDeviceOrchestrator;
    protected final XmlBuilder xmlBuilder = new XmlBuilder();

    public TestRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback, TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                      PerformanceTestManagementService performanceTestManagementService) {
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
    public TestReport run(TestTask testTask, TestRunDevice testRunDevice) {
        checkTestTaskCancel(testTask);
        TestRun testRun = setup(testTask, testRunDevice);
        checkTestTaskCancel(testTask);

        TestReport testReport = null;
        TestResult testResult = null;
        try {
            execute(testRun);
            checkTestTaskCancel(testTask);

            testResult = analyze(testRun);
            checkTestTaskCancel(testTask);

            testReport = report(testRun, testResult);
            checkTestTaskCancel(testTask);
        } catch (Exception e) {
            testRun.getLogger().error(testRunDevice.getDeviceInfo().getSerialNum() + ": " + e.getMessage(), e);
            saveErrorSummary(testRun, e);
        } finally {
            teardown(testRun);
            help(testRun, testResult);
        }
        return testReport;
    }

    @Override
    public TestRun setup(TestTask testTask, TestRunDevice testRunDevice) {
        return null;
    }

    @Override
    public void execute(TestRun testRun) throws Exception {

    }

    @Override
    public TestResult analyze(TestRun testRun) {
        return null;
    }

    @Override
    public TestReport report(TestRun testRun, TestResult testResult) {
        return null;
    }

    @Override
    public void teardown(TestRun testRun) {

    }

    @Override
    public void help(TestRun testRun, TestResult testResult) {

    }

    public void runTestOnDevice(TestTask testTask, TestRunDevice testRunDevice) {
        checkTestTaskCancel(testTask);
        Assert.notNull(testRunDevice.getLogger(), "testRunDevice.getLogger() is null, but it's required for a test run");
        testRunDevice.getLogger().info("Start running tests {}, timeout {}s", testTask.getTestSuite(), testTask.getTimeOutSecond());

        TestRun testRun = createTestRun(testRunDevice, testTask);
        checkTestTaskCancel(testTask);

        try {
            setUp(testRunDevice, testTask, testRun);
            checkTestTaskCancel(testTask);
            runByFutureTask(testRunDevice, testTask, testRun);
        } catch (Exception e) {
            testRun.getLogger().error(testRunDevice.getDeviceInfo().getSerialNum() + ": " + e.getMessage(), e);
            saveErrorSummary(testRun, e);
        } finally {
            tearDown(testRunDevice, testTask, testRun);
        }
    }

    private void runByFutureTask(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) throws Exception {
        FutureTask<String> futureTask = new FutureTask<>(() -> {
            loadTestRunToCurrentThread(testRun);
            run(testRunDevice, testTask, testRun);
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
            stopTest(testRunDevice);
            throw e;
        }
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
    }

    protected void checkTestTaskCancel(TestTask testTask) {
        Assert.isFalse(testTask.isCanceled(), "Task {} is canceled", testTask.getId());
    }

    protected TestRun createTestRun(TestRunDevice testRunDevice, TestTask testTask) {
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
        Logger loggerForTestRun = createLoggerForTestRun(testRun, testTask.getTestSuite(), parentLogger);
        testRun.setLogger(loggerForTestRun);
        testTask.addTestedDeviceResult(testRun);
        return testRun;
    }

    protected void setUp(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) throws Exception {
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
        testRunDeviceOrchestrator.getScreenShot(testRunDevice, agentManagementService.getScreenshotDir(), testRun.getLogger());

        if (performanceTestManagementService != null && testTask.getInspectionStrategies() != null) {
            for (InspectionStrategy strategy : testTask.getInspectionStrategies()) {
                performanceTestManagementService.inspectWithStrategy(strategy);
            }
        }
    }

    protected abstract void run(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) throws Exception;

    protected void tearDown(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) {
        // stop performance test
        if (performanceTestManagementService != null) {
            try {
                performanceTestManagementService.testTearDown(testRunDevice, testTask, testRun);
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
