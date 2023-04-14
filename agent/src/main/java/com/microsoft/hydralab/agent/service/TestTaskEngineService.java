package com.microsoft.hydralab.agent.service;

import com.microsoft.hydralab.agent.command.DeviceScriptCommandLoader;
import com.microsoft.hydralab.agent.config.TestRunnerConfig;
import com.microsoft.hydralab.agent.runner.DeviceTaskControlExecutor;
import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.agent.util.FileLoadUtil;
import com.microsoft.hydralab.common.entity.agent.DeviceTaskControl;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestRunDeviceCombo;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.device.DeviceType;
import com.microsoft.hydralab.common.util.AttachmentService;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.DateUtil;
import com.microsoft.hydralab.common.util.FileUtil;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service("TestTaskEngineService")
public class TestTaskEngineService implements TestTaskRunCallback {
    @Resource
    FileLoadUtil fileLoadUtil;
    @Resource
    TestDataService testDataService;
    @SuppressWarnings("constantname")
    static final Logger log = LoggerFactory.getLogger(TestTaskEngineService.class);
    @Resource
    ApplicationContext applicationContext;
    @Resource
    AttachmentService attachmentService;
    @Resource(name = "WebSocketClient")
    private TestTaskRunCallback webSocketCallback;
    @Resource
    DeviceTaskControlExecutor deviceTaskControlExecutor;
    @Resource
    AgentManagementService agentManagementService;
    @Resource
    DeviceScriptCommandLoader deviceScriptCommandLoader;
    @Resource
    TestRunDeviceOrchestrator testRunDeviceOrchestrator;
    private final Map<String, TestTask> runningTestTask = new HashMap<>();

    public TestTask runTestTask(TestTask testTask) {
        String beanName = TestRunnerConfig.testRunnerMap.get(testTask.getRunningType());
        TestRunner runner = applicationContext.getBean(beanName, TestRunner.class);

        Set<TestRunDevice> chosenDevices = chooseDevices(testTask);

        onTaskStart(testTask);
        DeviceTaskControl deviceTaskControl = deviceTaskControlExecutor.runForAllDeviceAsync(chosenDevices,
                new DeviceTaskControlExecutor.DeviceTask() {
                    @Override
                    public boolean doTask(TestRunDevice testRunDevice, Logger logger) throws Exception {
                        runner.runTestOnDevice(testTask, testRunDevice, logger);
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

    protected Set<TestRunDevice> chooseDevices(TestTask testTask) {
        String identifier = testTask.getDeviceIdentifier();

        String targetedDevicesDescriptor = testTask.getGroupDevices();
        if (!identifier.startsWith(Const.DeviceGroup.GROUP_NAME_PREFIX)) {
            targetedDevicesDescriptor = identifier;
        }
        Assert.notNull(targetedDevicesDescriptor, "No device found for is specified for " + testTask.getDeviceIdentifier());

        List<String> deviceSerials = Arrays.asList(targetedDevicesDescriptor.split(","));

        Set<DeviceInfo> allActiveConnectedDevice = agentManagementService.getActiveDeviceList(log);
        log.info("Choosing devices from {}", allActiveConnectedDevice.size());
        List<DeviceInfo> devices = allActiveConnectedDevice.stream()
                .filter(deviceInfo -> deviceSerials.contains(deviceInfo.getSerialNum()))
                .collect(Collectors.toList());

        Set<TestRunDevice> chosenDevices = new HashSet<>();
        if (devices.size() == 0) {
            log.error("No device found for " + targetedDevicesDescriptor);
            return chosenDevices;
        }

        String runningType = testTask.getRunningType();
        if (((runningType.equals(TestTask.TestRunningType.APPIUM_CROSS)) || (runningType.equals(TestTask.TestRunningType.T2C_JSON_TEST))) && devices.size() > 1) {
            Optional<DeviceInfo> mainDeviceInfo = devices.stream().filter(deviceInfo -> !DeviceType.WINDOWS.name().equals(deviceInfo.getType())).findFirst();
            Assert.isTrue(mainDeviceInfo.isPresent(), "There are more than 1 device, but all of them is windows device!");
            devices.remove(mainDeviceInfo.get());
            TestRunDeviceCombo testRunDeviceCombo = new TestRunDeviceCombo(mainDeviceInfo.get(), devices);
            chosenDevices.add(testRunDeviceCombo);
        } else {
            for (DeviceInfo deviceInfo : devices) {
                TestRunDevice testRunDevice = new TestRunDevice(deviceInfo, deviceInfo.getType());
                chosenDevices.add(testRunDevice);
            }
        }
        return chosenDevices;
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
        setupTestDir(testTask);
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
    public void onOneDeviceComplete(TestTask testTask, TestRunDevice testRunDevice, Logger logger, TestRun result) {
        log.info("onOneDeviceComplete: {}", testRunDevice.getDeviceInfo().getSerialNum());
        testRunDeviceOrchestrator.finishTask(testRunDevice);
        File deviceTestResultFolder = result.getResultFolder();

        File[] files = deviceTestResultFolder.listFiles();
        List<StorageFileInfo> attachments = new ArrayList<>();
        Assert.notNull(files, "should have result file to upload");
        for (File file : files) {
            if (file.isDirectory() && file.listFiles().length > 0) {
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
        return attachmentService.saveFileInStorageAndDB(storageFileInfo, file, EntityType.TEST_RESULT, logger);
    }

    private void processAndSaveDeviceTestResultBlobUrl(TestRun result) {
        Assert.isTrue(result.getAttachments().size() > 0, "deviceTestResultBlobUrl should not null");
        String deviceTestResultBlobUrl = result.getAttachments().get(0).getCDNUrl();
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
