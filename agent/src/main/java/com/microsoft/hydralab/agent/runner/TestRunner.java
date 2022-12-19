// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner;

import cn.hutool.core.lang.Assert;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.management.impl.IOSDeviceManager;
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
    protected final TestTaskRunCallback testTaskRunCallback;
    protected final XmlBuilder xmlBuilder = new XmlBuilder();

    public TestRunner(DeviceManager deviceManager, TestTaskRunCallback testTaskRunCallback) {
        this.deviceManager = deviceManager;
        this.testTaskRunCallback = testTaskRunCallback;
    }

    public void runTestOnDevice(TestTask testTask, DeviceInfo deviceInfo, Logger logger) throws Exception {
        checkTestTaskCancel(testTask);
        logger.info("Start running tests {}, timeout {}s", testTask.getTestSuite(), testTask.getTimeOutSecond());

        DeviceTestTask deviceTestTask = buildDeviceTestTask(deviceInfo, testTask, logger);
        checkTestTaskCancel(testTask);

        setUp(deviceInfo, testTask, deviceTestTask);
        checkTestTaskCancel(testTask);

        try {
            run(deviceInfo, testTask, deviceTestTask);
        } catch (Exception e) {
            deviceTestTask.getLogger().error(deviceInfo.getSerialNum() + ": " + e.getMessage(), e);
            saveErrorSummary(deviceTestTask, e);
        } finally {
            tearDown(deviceInfo, testTask, deviceTestTask);
        }
    }

    private static void saveErrorSummary(DeviceTestTask deviceTestTask, Exception e) {
        String errorStr = e.getClass().getName() + ": " + e.getMessage();
        if (errorStr.length() > 255) {
            errorStr = errorStr.substring(0, 254);
        }
        deviceTestTask.setErrorInProcess(errorStr);
    }

    protected void checkTestTaskCancel(TestTask testTask) {
        Assert.isFalse(testTask.isCanceled(), "Task {} is canceled", testTask.getId());
    }

    protected DeviceTestTask buildDeviceTestTask(DeviceInfo deviceInfo, TestTask testTask, Logger parentLogger) {
        DeviceTestTask deviceTestTask = new DeviceTestTask(deviceInfo.getSerialNum(), deviceInfo.getName(), testTask.getId());
        File deviceTestResultFolder = new File(testTask.getResourceDir(), deviceInfo.getSerialNum());
        parentLogger.info("DeviceTestResultFolder {}", deviceTestResultFolder);
        if (!deviceTestResultFolder.exists()) {
            if (!deviceTestResultFolder.mkdirs()) {
                throw new RuntimeException("deviceTestResultFolder.mkdirs() failed: " + deviceTestResultFolder);
            }
        }

        deviceTestTask.setDeviceTestResultFolder(deviceTestResultFolder);
        Logger loggerForDeviceTestTask = createLoggerForDeviceTestTask(deviceTestTask, testTask.getTestSuite(), parentLogger);
        deviceTestTask.setLogger(loggerForDeviceTestTask);
        testTask.addTestedDeviceResult(deviceTestTask);
        return deviceTestTask;
    }

    protected void setUp(DeviceInfo deviceInfo, TestTask testTask, DeviceTestTask deviceTestTask) throws Exception {
        deviceInfo.killAll();
        // this key will be used to recover device status when lost the connection between agent and master
        deviceInfo.addCurrentTask(testTask);

        /* set up device */
        deviceTestTask.getLogger().info("Start setup device");
        deviceManager.testDeviceSetup(deviceInfo, deviceTestTask.getLogger());
        deviceManager.wakeUpDevice(deviceInfo, deviceTestTask.getLogger());
        ThreadUtils.safeSleep(1000);
        checkTestTaskCancel(testTask);
        reInstallApp(deviceInfo, testTask, deviceTestTask.getLogger());
        reInstallTestApp(deviceInfo, testTask, deviceTestTask.getLogger());

        if (testTask.isThisForMicrosoftLauncher()) {
            presetForMicrosoftLauncherApp(deviceInfo, testTask, deviceTestTask.getLogger());
        }

        deviceTestTask.getLogger().info("Start granting all package needed permissions device");
        deviceManager.grantAllTaskNeededPermissions(deviceInfo, testTask, deviceTestTask.getLogger());

        checkTestTaskCancel(testTask);
        deviceManager.getScreenShot(deviceInfo, deviceTestTask.getLogger());
    }

    protected abstract void run(DeviceInfo deviceInfo, TestTask testTask, DeviceTestTask deviceTestTask) throws Exception;

    protected void tearDown(DeviceInfo deviceInfo, TestTask testTask, DeviceTestTask deviceTestTask) {
        try {
            String absoluteReportPath = xmlBuilder.buildTestResultXml(testTask, deviceTestTask);
            deviceTestTask.setTestXmlReportPath(deviceManager.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
        } catch (Exception e) {
            deviceTestTask.getLogger().error("Error in buildTestResultXml", e);
        }
        if (testTaskRunCallback != null) {
            try {
                testTaskRunCallback.onOneDeviceComplete(testTask, deviceInfo, deviceTestTask.getLogger(), deviceTestTask);
            } catch (Exception e) {
                deviceTestTask.getLogger().error("Error in onOneDeviceComplete", e);
            }
        }
        deviceManager.testDeviceUnset(deviceInfo, deviceTestTask.getLogger());
        deviceTestTask.getLogger().info("Start Close/finish resource");
        LogUtils.releaseLogger(deviceTestTask.getLogger());
    }

    private void presetForMicrosoftLauncherApp(DeviceInfo deviceInfo, TestTask testTask, Logger reportLogger) {
        reportLogger.info("PresetForMicrosoftLauncherApp: Start default launcher");
        deviceManager.setProperty(deviceInfo, "log.tag.WelcomeScreen", "VERBOSE", reportLogger);
        deviceManager.setProperty(deviceInfo, "log.tag.ConsentDialog", "VERBOSE", reportLogger);
        deviceManager.setProperty(deviceInfo, "log.tag.WhatsNewDialog", "VERBOSE", reportLogger);
        deviceManager.setProperty(deviceInfo, "log.tag.NoneCheckUpdates", "VERBOSE", reportLogger);

        if (!deviceManager.setLauncherAsDefault(deviceInfo, testTask.getPkgName(), testTask.getCurrentDefaultActivity(), reportLogger)) {
            testTask.switchDefaultActivity();
            deviceManager.setLauncherAsDefault(deviceInfo, testTask.getPkgName(), testTask.getCurrentDefaultActivity(), reportLogger);
        }
        reportLogger.info("Finish default launcher, currentDefaultActivity {}", testTask.getCurrentDefaultActivity());

        deviceManager.backToHome(deviceInfo, reportLogger);
        ThreadUtils.safeSleep(3000);
    }


    protected void reInstallApp(DeviceInfo deviceInfo, TestTask testTask, Logger reportLogger) throws Exception {
        if (testTask.getRequireReinstall() || deviceManager instanceof IOSDeviceManager) {
            deviceManager.uninstallApp(deviceInfo, testTask.getPkgName(), reportLogger);
            ThreadUtils.safeSleep(1000);
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
        }
        checkTestTaskCancel(testTask);
        deviceManager.installApp(deviceInfo, testTask.getTestAppFile().getAbsolutePath(), reportLogger);
    }

    protected boolean shouldInstallTestPackageAsApp() {
        return false;
    }

    private Logger createLoggerForDeviceTestTask(DeviceTestTask deviceTestTask, String loggerNamePrefix, Logger parentLogger) {
        parentLogger.info("Start setup report child parentLogger");
        String dateInfo = DateUtil.fileNameDateFormat.format(new Date());
        File instrumentLogFile = new File(deviceTestTask.getDeviceTestResultFolder(), loggerNamePrefix + "_" + dateInfo + ".log");
        // make sure it's a child logger of the parentLogger
        String loggerName = parentLogger.getName() + ".test." + dateInfo;
        Logger reportLogger = LogUtils.getLoggerWithRollingFileAppender(loggerName, instrumentLogFile.getAbsolutePath(), "%d %p -- %m%n");
        // TODO the getTestBaseRelPathInUrl method shouldn't be in deviceManager, testBaseDir should be managed by agent
        deviceTestTask.setInstrumentReportPath(deviceManager.getTestBaseRelPathInUrl(instrumentLogFile));

        return reportLogger;
    }

}
