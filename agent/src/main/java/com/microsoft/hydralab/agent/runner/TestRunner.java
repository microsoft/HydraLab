// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.agent.service.TestDataService;
import com.microsoft.hydralab.agent.util.FileLoadUtil;
import com.microsoft.hydralab.common.entity.agent.RunningControl;
import com.microsoft.hydralab.common.entity.center.TestTaskSpec;
import com.microsoft.hydralab.common.entity.common.*;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public abstract class TestRunner {
    protected Logger log = LoggerFactory.getLogger(DeviceManager.class);
    @Resource
    protected DeviceManager deviceManager;
    @Resource
    TestDataService testDataService;
    @Resource
    FileLoadUtil fileLoadUtil;
    @Resource
    RunningControlService runningControlService;
    @Resource
    protected
    AttachmentService attachmentService;
    @Resource(name = "WebSocketClient")
    private TestRunningCallback webSocketCallback;
    @Resource
    XmlBuilder xmlBuilder;

    protected abstract RunningControlService.DeviceTask getDeviceTask(TestTask testTask, TestRunningCallback testRunningCallback);

    public TestRunningCallback initCallback() {
        return new TestRunningCallback() {
            @Override
            public void onAllComplete(TestTask task) {
                fileLoadUtil.clearAttachments(task);
                if (task.isCanceled()) {
                    log.warn("test task {} is canceled, no data will be saved", task.getId());
                    return;
                }

                if (webSocketCallback != null) {
                    webSocketCallback.onAllComplete(task);
                }

                log.info("test task {} is completed, start to save info", task.getId());

                testDataService.saveTestTaskData(task, true);

            }

            @Override
            public void onOneDeviceComplete(TestTask task, DeviceInfo deviceControl, Logger logger, DeviceTestTask result) {
                log.info("onOneDeviceComplete: {}", deviceControl.getSerialNum());
                deviceControl.finishTask();
                File deviceTestResultFolder = result.getDeviceTestResultFolder();

                File[] files = deviceTestResultFolder.listFiles();
                List<BlobFileInfo> attachments = new ArrayList<>();
                Assert.notNull(files, "should have result file to upload");
                for (File file : files) {
                    if (file.isDirectory()) {
                        File zipFile = FileUtil.zipFile(file.getAbsolutePath(), deviceTestResultFolder + "/" + file.getName() + ".zip");
                        attachments.add(saveFileToBlob(zipFile, deviceTestResultFolder, logger));
                        continue;
                    }
                    attachments.add(saveFileToBlob(file, deviceTestResultFolder, logger));
                }
                result.setAttachments(attachments);
                processAndSaveDeviceTestResultBlobUrl(result);
            }

            @Override
            public void onDeviceOffline(TestTask testTask) {
                testTask.setStatus(TestTask.TestStatus.CANCELED);
                if (webSocketCallback != null) {
                    webSocketCallback.onDeviceOffline(testTask);
                }
                log.warn("device disconnected, test task {} will be re-queue, no data will be saved", testTask.getId());
            }

        };
    }

    private BlobFileInfo saveFileToBlob(File file, File folder, Logger logger) {
        BlobFileInfo blobFileInfo = new BlobFileInfo(file, "test/result/" + folder.getParentFile().getName() + "/" + folder.getName(), BlobFileInfo.FileType.COMMON_FILE);
        return attachmentService.addFileInfo(blobFileInfo, file, EntityFileRelation.EntityType.TEST_RESULT, logger);
    }

    protected Set<DeviceInfo> chooseDevices(TestTaskSpec testTaskSpec) {
        String identifier = testTaskSpec.deviceIdentifier;
        Set<DeviceInfo> allActiveConnectedDevice = deviceManager.getDeviceList(log);
        log.info("Choosing devices from {}", allActiveConnectedDevice.size());

        if (identifier.startsWith(Const.DeviceGroup.groupPre)) {
            List<String> devices = Arrays.asList(testTaskSpec.groupDevices.split(","));
            return allActiveConnectedDevice.stream().filter(adbDeviceInfo -> devices.contains(adbDeviceInfo.getSerialNum())).collect(Collectors.toSet());
        }

        return allActiveConnectedDevice.stream().filter(adbDeviceInfo -> identifier.equals(adbDeviceInfo.getSerialNum())).collect(Collectors.toSet());

    }

    public void setupTestDir(TestTask testTask) {
        File baseDir = new File(deviceManager.getTestBaseDir(), DateUtil.nowDirFormat.format(new Date()));
        if (!baseDir.exists()) {
            if (!baseDir.mkdirs()) {
                throw new RuntimeException("mkdirs fail for: " + baseDir);
            }
        }
        testTask.setTestCaseBaseDir(baseDir);
    }

    public TestTask runTest(TestTaskSpec testTaskSpec) {
        if (StringUtils.isBlank(testTaskSpec.testSuiteClass)) {
            testTaskSpec.testSuiteClass = testTaskSpec.pkgName;
        }
        Set<DeviceInfo> chosenDevices = chooseDevices(testTaskSpec);

        TestTask testTask = TestTask.convertToTestTask(testTaskSpec);

        fileLoadUtil.loadAttachments(testTask);
        setupTestDir(testTask);

        TestRunningCallback runningCallback = initCallback();
        RunningControlService.DeviceTask deviceTask = getDeviceTask(testTask, runningCallback);

        deviceManager.getRunningTestTask().put(testTask.getId(), testTask);
        RunningControl runningControl = runningControlService.runForAllDeviceAsync(chosenDevices, deviceTask, (devices) -> {
            testTask.onFinished();
            if (!testTask.isCanceled()) {
                testTask.setStatus(TestTask.TestStatus.FINISHED);
            }

            if (runningCallback != null) {
                runningCallback.onAllComplete(testTask);
            }
            deviceManager.getRunningTestTask().remove(testTask.getId());
        });
        if (runningControl == null) {
            testTask.setTestDevicesCount(0);
        } else {
            testTask.setTestDevicesCount(runningControl.devices.size());
        }
        return testTask;
    }


    private void processAndSaveDeviceTestResultBlobUrl(DeviceTestTask result) {
        Assert.isTrue(result.getAttachments().size() > 0, "deviceTestResultBlobUrl should not null");
        String deviceTestResultBlobUrl = result.getAttachments().get(0).getBlobUrl();
        String fileName = result.getAttachments().get(0).getFileName();
        log.info("deviceTestResultBlobUrl is {}", deviceTestResultBlobUrl);

        int start = deviceTestResultBlobUrl.lastIndexOf(fileName);
        deviceTestResultBlobUrl = deviceTestResultBlobUrl.substring(0, start);

        if (deviceTestResultBlobUrl.endsWith("%2F")) {
            deviceTestResultBlobUrl = deviceTestResultBlobUrl.substring(0, deviceTestResultBlobUrl.length() - 3);
        } else if (deviceTestResultBlobUrl.endsWith("/")) {
            deviceTestResultBlobUrl = deviceTestResultBlobUrl.substring(0, deviceTestResultBlobUrl.length() - 1);
        }

        log.info("After process: deviceTestResultBlobUrl is {}", deviceTestResultBlobUrl);
        result.setDeviceTestResultFolderUrl(deviceTestResultBlobUrl);
    }

    protected void checkTestTaskCancel(TestTask testTask) {
        cn.hutool.core.lang.Assert.isFalse(testTask.isCanceled(), "Task {} is canceled", testTask.getId());
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

    protected void afterTest(DeviceInfo deviceInfo, TestTask testTask, DeviceTestTask deviceTestTask, TestRunningCallback testRunningCallback, Logger reportLogger) {
        if (testRunningCallback != null) {
            try {
                String absoluteReportPath = xmlBuilder.buildTestResultXml(testTask, deviceTestTask);
                deviceTestTask.setTestXmlReportPath(deviceManager.getTestBaseRelPathInUrl(new File(absoluteReportPath)));
            } catch (Exception e) {
                reportLogger.error("Error in buildTestResultXml", e);
            }
            try {
                testRunningCallback.onOneDeviceComplete(testTask, deviceInfo, reportLogger, deviceTestTask);
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

    public Logger initReportLogger(DeviceTestTask deviceTestTask, TestTask testTask, Logger logger) {
        logger.info("Start setup report child logger");
        String dateInfo = DateUtil.fileNameDateFormat.format(new Date());
        File instrumentLogFile = new File(deviceTestTask.getDeviceTestResultFolder(), testTask.getTestSuite() + "_" + dateInfo + ".log");
        // make sure it's a child logger
        String loggerName = logger.getName() + ".test." + dateInfo;
        Logger reportLogger = LogUtils.getLoggerWithRollingFileAppender(loggerName, instrumentLogFile.getAbsolutePath(), "%d %p -- %m%n");
        deviceTestTask.setInstrumentReportPath(deviceManager.getTestBaseRelPathInUrl(instrumentLogFile));

        return reportLogger;
    }

    public DeviceTestTask initDeviceTestTask(DeviceInfo deviceInfo, TestTask testTask, Logger logger) {
        DeviceTestTask deviceTestTask = new DeviceTestTask(deviceInfo.getSerialNum(), deviceInfo.getName(), testTask.getId());
        File deviceTestResultFolder = new File(testTask.getTestCaseBaseDir(), deviceInfo.getSerialNum());
        logger.info("DeviceTestResultFolder {}", deviceTestResultFolder);
        if (!deviceTestResultFolder.exists()) {
            if (!deviceTestResultFolder.mkdirs()) {
                throw new RuntimeException("deviceTestResultFolder.mkdirs() failed: " + deviceTestResultFolder);
            }
        }
        deviceTestTask.setDeviceTestResultFolder(deviceTestResultFolder);
        return deviceTestTask;
    }
}
