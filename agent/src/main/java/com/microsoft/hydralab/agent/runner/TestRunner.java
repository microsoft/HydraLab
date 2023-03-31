// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner;

import cn.hutool.core.lang.Assert;
import com.microsoft.hydralab.common.entity.common.DeviceAction;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.device.TestDeviceManager;
import com.microsoft.hydralab.common.management.device.impl.IOSTestDeviceManager;
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

public abstract class TestRunner {
    protected final Logger log = LoggerFactory.getLogger(TestRunner.class);
    protected final AgentManagementService agentManagementService;
    protected final TestTaskRunCallback testTaskRunCallback;
    protected final PerformanceTestManagementService performanceTestManagementService;
    protected final XmlBuilder xmlBuilder = new XmlBuilder();
    protected final ActionExecutor actionExecutor = new ActionExecutor();
    protected TestDeviceManager testDeviceManager;

    public TestRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback,
                      PerformanceTestManagementService performanceTestManagementService) {
        this.agentManagementService = agentManagementService;
        this.testTaskRunCallback = testTaskRunCallback;
        this.performanceTestManagementService = performanceTestManagementService;
    }

    public void runTestOnDevice(TestTask testTask, DeviceInfo deviceInfo, Logger logger) {
        checkTestTaskCancel(testTask);
        logger.info("Start running tests {}, timeout {}s", testTask.getTestSuite(), testTask.getTimeOutSecond());

        TestRun testRun = createTestRun(deviceInfo, testTask, logger);
        testDeviceManager = deviceInfo.getTestDeviceManager();
        checkTestTaskCancel(testTask);

        try {
            setUp(deviceInfo, testTask, testRun);
            checkTestTaskCancel(testTask);
            runByFutureTask(deviceInfo, testTask, testRun);
        } catch (Exception e) {
            testRun.getLogger().error(deviceInfo.getSerialNum() + ": " + e.getMessage(), e);
            saveErrorSummary(testRun, e);
        } finally {
            tearDown(deviceInfo, testTask, testRun);
        }
    }

    private void runByFutureTask(DeviceInfo deviceInfo, TestTask testTask, TestRun testRun) throws Exception {
        FutureTask<String> futureTask = new FutureTask<>(() -> {
            loadTestRunToCurrentThread(testRun);
            run(deviceInfo, testTask, testRun);
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
            stopTest(deviceInfo);
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

    protected TestRun createTestRun(DeviceInfo deviceInfo, TestTask testTask, Logger parentLogger) {
        TestRun testRun = new TestRun(deviceInfo.getSerialNum(), deviceInfo.getName(), testTask.getId());
        File testRunResultFolder = new File(testTask.getResourceDir(), deviceInfo.getSerialNum());
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

    protected void setUp(DeviceInfo deviceInfo, TestTask testTask, TestRun testRun) throws Exception {
        deviceInfo.killAll();
        // this key will be used to recover device status when lost the connection between agent and master
        deviceInfo.addCurrentTask(testTask);
        loadTestRunToCurrentThread(testRun);
        /* set up device */
        testRun.getLogger().info("Start setup device");
        testDeviceManager.testDeviceSetup(deviceInfo, testRun.getLogger());
        testDeviceManager.wakeUpDevice(deviceInfo, testRun.getLogger());
        ThreadUtils.safeSleep(1000);
        checkTestTaskCancel(testTask);
        reInstallApp(deviceInfo, testTask, testRun.getLogger());
        reInstallTestApp(deviceInfo, testTask, testRun.getLogger());

        //execute actions
        if (testTask.getDeviceActions() != null) {
            testRun.getLogger().info("Start executing setUp actions.");
            List<Exception> exceptions = actionExecutor.doActions(testDeviceManager, deviceInfo, testRun.getLogger(),
                    testTask.getDeviceActions(), DeviceAction.When.SET_UP);
            Assert.isTrue(exceptions.size() == 0, () -> exceptions.get(0));
        }

        testRun.getLogger().info("Start granting all package needed permissions device");
        testDeviceManager.grantAllTaskNeededPermissions(deviceInfo, testTask, testRun.getLogger());

        checkTestTaskCancel(testTask);
        testDeviceManager.getScreenShot(deviceInfo, testRun.getLogger());

        if (performanceTestManagementService != null && testTask.getInspectionStrategies() != null) {
            for (InspectionStrategy strategy : testTask.getInspectionStrategies()) {
                performanceTestManagementService.inspectWithStrategy(strategy);
            }
        }
    }

    protected abstract void run(DeviceInfo deviceInfo, TestTask testTask, TestRun testRun) throws Exception;

    protected void tearDown(DeviceInfo deviceInfo, TestTask testTask, TestRun testRun) {
        // stop performance test
        if (performanceTestManagementService != null) {
            try {
                performanceTestManagementService.testTearDown(deviceInfo, testTask, testRun, log);
            } catch (Exception e) {
                testRun.getLogger().error("Error in performance test tearDown", e);
            }
        }

        //execute actions
        if (testTask.getDeviceActions() != null) {
            testRun.getLogger().info("Start executing tearDown actions.");
            List<Exception> exceptions = actionExecutor.doActions(testDeviceManager, deviceInfo, testRun.getLogger(),
                    testTask.getDeviceActions(), DeviceAction.When.TEAR_DOWN);
            if (exceptions.size() > 0) {
                testRun.getLogger().error("Execute actions failed when tearDown!", exceptions.get(0));
            }
        }
        testDeviceManager.testDeviceUnset(deviceInfo, testRun.getLogger());

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
                testTaskRunCallback.onOneDeviceComplete(testTask, deviceInfo, testRun.getLogger(), testRun);
            } catch (Exception e) {
                testRun.getLogger().error("Error in onOneDeviceComplete", e);
            }
        }
        testRun.getLogger().info("Start Close/finish resource");
        LogUtils.releaseLogger(testRun.getLogger());
    }

    protected void reInstallApp(DeviceInfo deviceInfo, TestTask testTask, Logger reportLogger) throws Exception {
        if (testTask.getRequireReinstall() || testDeviceManager instanceof IOSTestDeviceManager) {
            testDeviceManager.uninstallApp(deviceInfo, testTask.getPkgName(), reportLogger);
            ThreadUtils.safeSleep(3000);
        } else if (testTask.getRequireClearData()) {
            testDeviceManager.resetPackage(deviceInfo, testTask.getPkgName(), reportLogger);
        }
        checkTestTaskCancel(testTask);
        try {
            FlowUtil.retryAndSleepWhenFalse(3, 10, () -> testDeviceManager.installApp(deviceInfo, testTask.getAppFile().getAbsolutePath(), reportLogger));
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    protected void reInstallTestApp(DeviceInfo deviceInfo, TestTask testTask, Logger reportLogger)
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
        if (testTask.getRequireReinstall()) {
            testDeviceManager.uninstallApp(deviceInfo, testTask.getTestPkgName(), reportLogger);
            // test package uninstall should be faster than app package removal.
            ThreadUtils.safeSleep(2000);
        }
        checkTestTaskCancel(testTask);
        try {
            FlowUtil.retryAndSleepWhenFalse(3, 10, () -> testDeviceManager.installApp(deviceInfo, testTask.getTestAppFile().getAbsolutePath(), reportLogger));
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

    public void stopTest(DeviceInfo deviceInfo) {
        deviceInfo.killAll();
    }
}
