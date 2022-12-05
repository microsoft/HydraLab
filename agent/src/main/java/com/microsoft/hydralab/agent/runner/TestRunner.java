// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner;

import cn.hutool.core.lang.Assert;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.util.DateUtil;
import com.microsoft.hydralab.common.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;

@Service
public abstract class TestRunner {
    protected Logger log = LoggerFactory.getLogger(DeviceManager.class);
    @Resource
    protected DeviceManager deviceManager;
    @Resource
    XmlBuilder xmlBuilder;
    @Resource(name = "TestTaskEngineService")
    protected TestTaskRunCallback testTaskRunCallback;

    public abstract void runTestOnDevice(TestTask testTask, DeviceInfo deviceInfo, Logger logger);


    protected void checkTestTaskCancel(TestTask testTask) {
        Assert.isFalse(testTask.isCanceled(), "Task {} is canceled", testTask.getId());
    }

    public void initDevice(DeviceInfo deviceInfo, TestTask testTask, Logger reportLogger) throws Exception {
        deviceInfo.killAll();
        // this key will be used to recover device status when lost the connection between agent and master
        deviceInfo.addCurrentTask(testTask);

        /* set up device */
        reportLogger.info("Start setup device");
        deviceManager.testDeviceSetup(deviceInfo, reportLogger);
        deviceManager.wakeUpDevice(deviceInfo, reportLogger);
        deviceManager.safeSleep(1000);
        checkTestTaskCancel(testTask);
        reInstallApp(deviceInfo, testTask, reportLogger);

        if (testTask.isThisForMicrosoftLauncher()) {
            presetForMicrosoftLauncherApp(deviceInfo, testTask, reportLogger);
        }

        reportLogger.info("Start granting all package needed permissions device");
        deviceManager.grantAllTaskNeededPermissions(deviceInfo, testTask, reportLogger);

        checkTestTaskCancel(testTask);
        deviceManager.getScreenShot(deviceInfo, log);
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
        deviceManager.safeSleep(3000);
    }

    protected void afterTest(DeviceInfo deviceInfo, TestTask testTask, DeviceTestTask deviceTestTask, TestTaskRunCallback testTaskRunCallback, Logger reportLogger) {
        if (testTaskRunCallback != null) {
            try {
                String absoluteReportPath = xmlBuilder.buildTestResultXml(testTask, deviceTestTask);
                deviceTestTask.setTestXmlReportPath(deviceManager.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
            } catch (Exception e) {
                reportLogger.error("Error in buildTestResultXml", e);
            }
            try {
                testTaskRunCallback.onOneDeviceComplete(testTask, deviceInfo, reportLogger, deviceTestTask);
            } catch (Exception e) {
                reportLogger.error("Error in onOneDeviceComplete", e);
            }
        }
        deviceManager.testDeviceUnset(deviceInfo, reportLogger);
        reportLogger.info("Finally: restore state");
        /* Close/finish resource */
        if (reportLogger != null) {
            reportLogger.info("Start Close/finish resource");
            LogUtils.releaseLogger(reportLogger);
        }
    }

    public void reInstallApp(DeviceInfo deviceInfo, TestTask testTask, Logger reportLogger) throws Exception {
        if (testTask.getRequireReinstall()) {
            deviceManager.uninstallApp(deviceInfo, testTask.getPkgName(), reportLogger);
            deviceManager.uninstallApp(deviceInfo, testTask.getTestPkgName(), reportLogger);
            deviceManager.safeSleep(1000);
        } else if (testTask.getRequireClearData()) {
            deviceManager.resetPackage(deviceInfo, testTask.getPkgName(), reportLogger);
        }
        checkTestTaskCancel(testTask);

        deviceManager.installApp(deviceInfo, testTask.getAppFile().getAbsolutePath(), reportLogger);
        deviceManager.installApp(deviceInfo, testTask.getTestAppFile().getAbsolutePath(), reportLogger);

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

    protected DeviceTestTask initDeviceTestTask(DeviceInfo deviceInfo, TestTask testTask, Logger logger) {
        DeviceTestTask deviceTestTask = new DeviceTestTask(deviceInfo.getSerialNum(), deviceInfo.getName(), testTask.getId());
        File deviceTestResultFolder = new File(testTask.getResourceDir(), deviceInfo.getSerialNum());
        logger.info("DeviceTestResultFolder {}", deviceTestResultFolder);
        if (!deviceTestResultFolder.exists()) {
            if (!deviceTestResultFolder.mkdirs()) {
                throw new RuntimeException("deviceTestResultFolder.mkdirs() failed: " + deviceTestResultFolder);
            }
        }
        deviceTestTask.setDeviceTestResultFolder(deviceTestResultFolder);
        deviceTestTask.setLogger(createLoggerForDeviceTestTask(deviceTestTask, testTask.getTestSuite(), logger));
        return deviceTestTask;
    }
}
