package com.microsoft.hydralab.agent.service;

import com.microsoft.hydralab.agent.command.DeviceScriptCommandLoader;
import com.microsoft.hydralab.agent.config.TestRunnerConfig;
import com.microsoft.hydralab.agent.runner.DeviceTaskControlExecutor;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.agent.runner.appium.AppiumCrossRunner;
import com.microsoft.hydralab.agent.runner.t2c.T2CRunner;
import com.microsoft.hydralab.agent.util.FileLoadUtil;
import com.microsoft.hydralab.common.entity.agent.DeviceTaskControl;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.entity.common.TestTaskSpec;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.util.AttachmentService;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.DateUtil;
import com.microsoft.hydralab.common.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service("TestTaskEngineService")
public class TestTaskEngineService implements TestTaskRunCallback {
    @SuppressWarnings("constantname")
    static final Logger log = LoggerFactory.getLogger(TestTaskEngineService.class);
    private final Map<String, TestTask> runningTestTask = new HashMap<>();
    @Resource
    FileLoadUtil fileLoadUtil;
    @Resource
    TestDataService testDataService;
    @Resource
    ApplicationContext applicationContext;
    @Resource
    AttachmentService attachmentService;
    @Resource
    DeviceTaskControlExecutor deviceTaskControlExecutor;
    @Resource
    AgentManagementService agentManagementService;
    @Resource
    DeviceScriptCommandLoader deviceScriptCommandLoader;
    @Resource(name = "WebSocketClient")
    private TestTaskRunCallback webSocketCallback;

    public TestTask runTestTask(TestTaskSpec testTaskSpec) {
        updateTaskSpecWithDefaultValues(testTaskSpec);
        log.info("TestTaskSpec: {}", testTaskSpec);
        TestTask testTask = TestTask.convertToTestTask(testTaskSpec);
        setupTestDir(testTask);

        String beanName = TestRunnerConfig.testRunnerMap.get(testTaskSpec.runningType);
        TestRunner runner = applicationContext.getBean(beanName, TestRunner.class);

        Set<DeviceInfo> chosenDevices = chooseDevices(testTaskSpec, runner);

        onTaskStart(testTask);
        DeviceTaskControl deviceTaskControl = deviceTaskControlExecutor.runForAllDeviceAsync(chosenDevices,
                new DeviceTaskControlExecutor.DeviceTask() {
                    @Override
                    public boolean doTask(DeviceInfo deviceInfo, Logger logger) throws Exception {
                        runner.runTestOnDevice(testTask, deviceInfo, logger);
                        return false;
                    }
                },
                () -> {
                    testTask.onFinished();
                    if (!testTask.isCanceled()) {
                        testTask.setStatus(TestTask.TestStatus.FINISHED);
                    }

                    onTaskComplete(testTask);
                });

        if (deviceTaskControl == null) {
            testTask.setTestDevicesCount(0);
        } else {
            testTask.setTestDevicesCount(deviceTaskControl.devices.size());
        }
        return testTask;
    }

    private void updateTaskSpecWithDefaultValues(TestTaskSpec testTaskSpec) {
        determineScopeOfTestCase(testTaskSpec);

        if (StringUtils.isEmpty(testTaskSpec.runningType)) {
            testTaskSpec.runningType = TestTask.TestRunningType.INSTRUMENTATION;
        }
        if (StringUtils.isBlank(testTaskSpec.testSuiteClass)) {
            testTaskSpec.testSuiteClass = testTaskSpec.pkgName;
        }
    }

    protected Set<DeviceInfo> chooseDevices(TestTaskSpec testTaskSpec, TestRunner runner) {
        if ((runner instanceof AppiumCrossRunner) || (runner instanceof T2CRunner)) {
            Set<DeviceInfo> activeDeviceList = agentManagementService.getActiveDeviceList(log);
            Assert.isTrue(activeDeviceList == null || activeDeviceList.size() <= 1, "No connected device!");
            return activeDeviceList;
        }

        String identifier = testTaskSpec.deviceIdentifier;
        Set<DeviceInfo> allActiveConnectedDevice = agentManagementService.getDeviceList(log);
        log.info("Choosing devices from {}", allActiveConnectedDevice.size());

        if (identifier.startsWith(Const.DeviceGroup.GROUP_NAME_PREFIX)) {
            List<String> devices = Arrays.asList(testTaskSpec.groupDevices.split(","));
            return allActiveConnectedDevice.stream()
                    .filter(adbDeviceInfo -> devices.contains(adbDeviceInfo.getSerialNum()))
                    .collect(Collectors.toSet());
        }

        return allActiveConnectedDevice.stream()
                .filter(adbDeviceInfo -> identifier.equals(adbDeviceInfo.getSerialNum()))
                .collect(Collectors.toSet());
    }

    public void setupTestDir(TestTask testTask) {
        File baseDir = new File(agentManagementService.getTestBaseDir(), DateUtil.nowDirFormat.format(new Date()));
        if (!baseDir.exists()) {
            if (!baseDir.mkdirs()) {
                throw new RuntimeException("mkdirs fail for: " + baseDir);
            }
        }
        testTask.setResourceDir(baseDir);
    }

    private void determineScopeOfTestCase(TestTaskSpec testTaskSpec) {
        if (!StringUtils.isEmpty(testTaskSpec.testScope)) {
            return;
        }
        testTaskSpec.testScope = TestTask.TestScope.CLASS;
        if (StringUtils.isEmpty(testTaskSpec.testSuiteClass)) {
            testTaskSpec.testScope = TestTask.TestScope.TEST_APP;
        }
    }

    public Map<String, TestTask> getRunningTestTask() {
        return runningTestTask;
    }

    public boolean cancelTestTaskById(String testId) {
        final Map<String, TestTask> runningTestTask = getRunningTestTask();
        final TestTask testTask = runningTestTask.get(testId);
        if (testTask == null || testTask.isCanceled()) {
            return false;
        }
        testTask.setStatus(TestTask.TestStatus.CANCELED);
        agentManagementService.resetDeviceByTestId(testId, log);
        return true;
    }

    @Override
    public void onTaskStart(TestTask testTask) {
        fileLoadUtil.loadAttachments(testTask);
        deviceScriptCommandLoader.loadCommandAction(testTask);
        runningTestTask.put(testTask.getId(), testTask);
    }

    @Override
    public void onTaskComplete(TestTask testTask) {
        fileLoadUtil.clearAttachments(testTask);
        if (testTask.isCanceled()) {
            log.warn("test task {} is canceled, no data will be saved", testTask.getId());
            return;
        }

        if (webSocketCallback != null) {
            webSocketCallback.onTaskComplete(testTask);
        }

        log.info("test task {} is completed, start to save info", testTask.getId());

        testDataService.saveTestTaskData(testTask);
        runningTestTask.remove(testTask.getId());
    }

    @Override
    public void onOneDeviceComplete(TestTask testTask, DeviceInfo deviceControl, Logger logger, TestRun result) {
        log.info("onOneDeviceComplete: {}", deviceControl.getSerialNum());
        deviceControl.finishTask();
        File deviceTestResultFolder = result.getResultFolder();

        File[] files = deviceTestResultFolder.listFiles();
        List<StorageFileInfo> attachments = new ArrayList<>();
        Assert.notNull(files, "should have result file to upload");
        for (File file : files) {
            if (file.isDirectory()) {
                File zipFile = FileUtil.zipFile(file.getAbsolutePath(),
                        deviceTestResultFolder + "/" + file.getName() + ".zip");
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

    private StorageFileInfo saveFileToBlob(File file, File folder, Logger logger) {
        StorageFileInfo storageFileInfo = new StorageFileInfo(file,
                "test/result/" + folder.getParentFile().getName() + "/" + folder.getName(),
                StorageFileInfo.FileType.COMMON_FILE);
        return attachmentService.addFileInfo(storageFileInfo, file, EntityType.TEST_RESULT,
                logger);
    }

    private void processAndSaveDeviceTestResultBlobUrl(TestRun result) {
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
}
