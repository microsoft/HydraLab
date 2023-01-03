// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner;

import cn.hutool.core.lang.Assert;
import com.microsoft.hydralab.TestRunThreadContext;
import com.microsoft.hydralab.common.entity.common.DeviceAction;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.management.impl.IOSDeviceManager;
import com.microsoft.hydralab.performance.PerformanceInspectorManagementService;
import com.microsoft.hydralab.common.util.DateUtil;
import com.microsoft.hydralab.common.util.LogUtils;
import com.microsoft.hydralab.common.util.ThreadUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;

public abstract class TestRunner {
    protected final Logger log = LoggerFactory.getLogger(DeviceManager.class);
    protected final DeviceManager deviceManager;
    protected final PerformanceInspectorManagementService performanceInspectorManagementService;
    protected final TestTaskRunCallback testTaskRunCallback;
    protected final XmlBuilder xmlBuilder = new XmlBuilder();
    protected final ActionExecutor actionExecutor = new ActionExecutor();

    public TestRunner(DeviceManager deviceManager, TestTaskRunCallback testTaskRunCallback, PerformanceInspectorManagementService performanceInspectorManagementService) {
        this.deviceManager = deviceManager;
        this.performanceInspectorManagementService = performanceInspectorManagementService;
        this.testTaskRunCallback = testTaskRunCallback;
    }

    public void runTestOnDevice(TestTask testTask, DeviceInfo deviceInfo, Logger logger) throws Exception {
        checkTestTaskCancel(testTask);
        logger.info("Start running tests {}, timeout {}s", testTask.getTestSuite(), testTask.getTimeOutSecond());

        TestRun testRun = createTestRun(deviceInfo, testTask, logger);
        TestRunThreadContext.init(testRun, null, null);
        checkTestTaskCancel(testTask);

        setUp(deviceInfo, testTask, testRun);
        checkTestTaskCancel(testTask);

        try {
            run(deviceInfo, testTask, testRun);
        } catch (Exception e) {
            testRun.getLogger().error(deviceInfo.getSerialNum() + ": " + e.getMessage(), e);
            saveErrorSummary(testRun, e);
        } finally {
            // TODO List<PerformanceResult<?>> performanceResults = performanceInspectionService.parse();
            tearDown(deviceInfo, testTask, testRun);
        }
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
        File deviceTestResultFolder = new File(testTask.getResourceDir(), deviceInfo.getSerialNum());
        parentLogger.info("DeviceTestResultFolder {}", deviceTestResultFolder);
        if (!deviceTestResultFolder.exists()) {
            if (!deviceTestResultFolder.mkdirs()) {
                throw new RuntimeException("deviceTestResultFolder.mkdirs() failed: " + deviceTestResultFolder);
            }
        }

        testRun.setTestRunResultFolder(deviceTestResultFolder);
        Logger loggerForDeviceTestTask = createLoggerForDeviceTestTask(testRun, testTask.getTestSuite(), parentLogger);
        testRun.setLogger(loggerForDeviceTestTask);
        testTask.addTestedDeviceResult(testRun);
        return testRun;
    }

    protected void setUp(DeviceInfo deviceInfo, TestTask testTask, TestRun testRun) throws Exception {
        // TODO add the performance service initialization call setup folder and link the testTask and perfSpec

        deviceInfo.killAll();
        // this key will be used to recover device status when lost the connection between agent and master
        deviceInfo.addCurrentTask(testTask);

        /* set up device */
        testRun.getLogger().info("Start setup device");
        deviceManager.testDeviceSetup(deviceInfo, testRun.getLogger());
        deviceManager.wakeUpDevice(deviceInfo, testRun.getLogger());
        ThreadUtils.safeSleep(1000);
        checkTestTaskCancel(testTask);
        reInstallApp(deviceInfo, testTask, testRun.getLogger());
        reInstallTestApp(deviceInfo, testTask, testRun.getLogger());

        if (testTask.isThisForMicrosoftLauncher()) {
            presetForMicrosoftLauncherApp(deviceInfo, testTask, testRun.getLogger());
        }
        //execute actions
        if (testTask.getDeviceActions() != null) {
            testRun.getLogger().info("Start executing setUp actions.");
            actionExecutor.doActions(deviceManager, deviceInfo, testRun.getLogger(), testTask.getDeviceActions(), DeviceAction.When.SET_UP);
        }

        testRun.getLogger().info("Start granting all package needed permissions device");
        deviceManager.grantAllTaskNeededPermissions(deviceInfo, testTask, testRun.getLogger());

        checkTestTaskCancel(testTask);
        deviceManager.getScreenShot(deviceInfo, testRun.getLogger());
    }

    protected abstract void run(DeviceInfo deviceInfo, TestTask testTask, TestRun testRun) throws Exception;

    protected void tearDown(DeviceInfo deviceInfo, TestTask testTask, TestRun testRun) {
        try {
            String absoluteReportPath = xmlBuilder.buildTestResultXml(testTask, testRun);
            testRun.setTestXmlReportPath(deviceManager.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
        } catch (Exception e) {
            testRun.getLogger().error("Error in buildTestResultXml", e);
        }
        if (testTaskRunCallback != null) {
            try {
                testTaskRunCallback.onOneDeviceComplete(testTask, deviceInfo, testRun.getLogger(), testRun);
            } catch (Exception e) {
                testRun.getLogger().error("Error in onOneDeviceComplete", e);
            }
        }
        deviceManager.testDeviceUnset(deviceInfo, testRun.getLogger());
        //execute actions
        if (testTask.getDeviceActions() != null) {
            testRun.getLogger().info("Start executing tearDown actions.");
            actionExecutor.doActions(deviceManager, deviceInfo, testRun.getLogger(), testTask.getDeviceActions(), DeviceAction.When.TEAR_DOWN);
        }

        testRun.getLogger().info("Start Close/finish resource");
        LogUtils.releaseLogger(testRun.getLogger());
    }

    private void presetForMicrosoftLauncherApp(DeviceInfo deviceInfo, TestTask testTask, Logger reportLogger) {
        reportLogger.info("PresetForMicrosoftLauncherApp: Start default launcher");
        deviceManager.setProperty(deviceInfo, "log.tag.WelcomeScreen", "VERBOSE", reportLogger);
        deviceManager.setProperty(deviceInfo, "log.tag.ConsentDialog", "VERBOSE", reportLogger);
        deviceManager.setProperty(deviceInfo, "log.tag.WhatsNewDialog", "VERBOSE", reportLogger);
        deviceManager.setProperty(deviceInfo, "log.tag.NoneCheckUpdates", "VERBOSE", reportLogger);

        if (!deviceManager.setDefaultLauncher(deviceInfo, testTask.getPkgName(), testTask.getCurrentDefaultActivity(), reportLogger)) {
            testTask.switchDefaultActivity();
            deviceManager.setDefaultLauncher(deviceInfo, testTask.getPkgName(), testTask.getCurrentDefaultActivity(), reportLogger);
        }
        reportLogger.info("Finish default launcher, currentDefaultActivity {}", testTask.getCurrentDefaultActivity());

        deviceManager.backToHome(deviceInfo, reportLogger);
        ThreadUtils.safeSleep(3000);
    }


    protected void reInstallApp(DeviceInfo deviceInfo, TestTask testTask, Logger reportLogger) throws Exception {
        if (testTask.getRequireReinstall() || deviceManager instanceof IOSDeviceManager) {
            deviceManager.uninstallApp(deviceInfo, testTask.getPkgName(), reportLogger);
            ThreadUtils.safeSleep(3000);
        } else if (testTask.getRequireClearData()) {
            deviceManager.resetPackage(deviceInfo, testTask.getPkgName(), reportLogger);
        }
        checkTestTaskCancel(testTask);

        deviceManager.installApp(deviceInfo, testTask.getAppFile().getAbsolutePath(), reportLogger);
    }

    protected void reInstallTestApp(DeviceInfo deviceInfo, TestTask testTask, Logger reportLogger) throws Exception {
        if(!shouldInstallTestPackageAsApp()){
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
            deviceManager.uninstallApp(deviceInfo, testTask.getTestPkgName(), reportLogger);
            // test package uninstall should be faster than app package removal.
            ThreadUtils.safeSleep(2000);
        }
        checkTestTaskCancel(testTask);
        deviceManager.installApp(deviceInfo, testTask.getTestAppFile().getAbsolutePath(), reportLogger);
    }

    protected boolean shouldInstallTestPackageAsApp() {
        return false;
    }

    private Logger createLoggerForDeviceTestTask(TestRun testRun, String loggerNamePrefix, Logger parentLogger) {
        parentLogger.info("Start setup report child parentLogger");
        String dateInfo = DateUtil.fileNameDateFormat.format(new Date());
        File instrumentLogFile = new File(testRun.getTestRunResultFolder(), loggerNamePrefix + "_" + dateInfo + ".log");
        // make sure it's a child logger of the parentLogger
        String loggerName = parentLogger.getName() + ".test." + dateInfo;
        Logger reportLogger = LogUtils.getLoggerWithRollingFileAppender(loggerName, instrumentLogFile.getAbsolutePath(), "%d %p -- %m%n");
        // TODO the getTestBaseRelPathInUrl method shouldn't be in deviceManager, testBaseDir should be managed by agent
        testRun.setInstrumentReportPath(deviceManager.getTestBaseRelPathInUrl(instrumentLogFile));

        return reportLogger;
    }

}
