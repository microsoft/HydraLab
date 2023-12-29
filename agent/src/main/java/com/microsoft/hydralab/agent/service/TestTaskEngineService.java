package com.microsoft.hydralab.agent.service;

import com.microsoft.hydralab.agent.command.DeviceScriptCommandLoader;
import com.microsoft.hydralab.agent.runner.DeviceTaskControlExecutor;
import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.agent.runner.TestRunnerManager;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.agent.util.FileLoadUtil;
import com.microsoft.hydralab.common.entity.agent.DeviceTaskControl;
import com.microsoft.hydralab.common.entity.common.AnalysisTask;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.entity.common.Task;
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
    @Resource
    private TestRunnerManager testRunnerManager;
    private final Map<String, Task> runningTestTask = new HashMap<>();

    public TestTaskEngineService() {
    }

    public Task runTestTask(Task task) {
        Set<TestRunDevice> chosenDevices = chooseDevices(task);
        if (chosenDevices.size() == 0) {
            handleNoAvailableDevice(task);
            return task;
        }

        onTaskStart(task);
        DeviceTaskControl deviceTaskControl = deviceTaskControlExecutor.runForAllDeviceAsync(chosenDevices,
                testRunDevice -> {
                    testRunnerManager.runTestTask(task, testRunDevice);
                    return false;
                },
                () -> {
                    task.onFinished();
                    if (!task.isCanceled()) {
                        task.setStatus(Task.TaskStatus.FINISHED);
                    }

                    onTaskComplete(task);
                }, task instanceof AnalysisTask);

        if (deviceTaskControl == null) {
            handleNoAvailableDevice(task);
        } else {
            task.setDeviceCount(deviceTaskControl.devices.size());
        }
        return task;
    }

    private static void handleNoAvailableDevice(Task task) {
        TestTask testTask = (TestTask) task;
        testTask.setDeviceCount(0);
        testTask.setStatus(Task.TaskStatus.CANCELED);
        log.warn("No available device found for {}, group devices: {}, task {} is canceled on this agent",
                testTask.getDeviceIdentifier(), testTask.getGroupDevices(), testTask.getId());
    }

    protected Set<TestRunDevice> chooseDevices(Task task) {
        if (task instanceof AnalysisTask) {
            DeviceInfo fakeDeviceInfo = new DeviceInfo();
            fakeDeviceInfo.setSerialNum(task.getDeviceIdentifier());
            fakeDeviceInfo.setName("agent_task");
            TestRunDevice fakeDevice = new TestRunDevice(fakeDeviceInfo, "agent_task");
            return Set.of(fakeDevice);
        }
        TestTask testTask = (TestTask) task;
        String identifier = testTask.getDeviceIdentifier();

        String targetedDevicesDescriptor;
        if (identifier.startsWith(Const.DeviceGroup.GROUP_NAME_PREFIX)) {
            targetedDevicesDescriptor = testTask.getGroupDevices();
        } else {
            targetedDevicesDescriptor = identifier;
        }

        Assert.notNull(targetedDevicesDescriptor, "targetedDevicesDescriptor is null: "
                + testTask.getDeviceIdentifier() + ", group devices: " + testTask.getGroupDevices());

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

        String runningType = testTask.getRunnerType();
        if (((Task.RunnerType.APPIUM_CROSS.name().equals(runningType)) || (Task.RunnerType.T2C_JSON.name().equals(runningType))) && devices.size() > 1) {
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

    public void setupTestDir(Task task) {
        File baseDir = new File(agentManagementService.getTestBaseDir(), DateUtil.nowDirFormat.format(new Date()));
        if (!baseDir.exists()) {
            if (!baseDir.mkdirs()) {
                throw new RuntimeException("mkdirs fail for: " + baseDir);
            }
        }
        task.setResourceDir(baseDir);
    }

    public Map<String, Task> getRunningTestTask() {
        return runningTestTask;
    }

    public boolean cancelTestTaskById(String testId) {
        final Map<String, Task> runningTestTask = getRunningTestTask();
        final Task task = runningTestTask.get(testId);
        if (task == null || task.isCanceled()) {
            return false;
        }
        task.setStatus(Task.TaskStatus.CANCELED);
        agentManagementService.resetDeviceByTestId(testId, log);
        return true;
    }

    @Override
    public void onTaskStart(Task task) {
        setupTestDir(task);
        fileLoadUtil.loadAttachments(task);
        deviceScriptCommandLoader.loadCommandAction(task);
        runningTestTask.put(task.getId(), task);
    }

    @Override
    public void onTaskComplete(Task task) {
        try {
            fileLoadUtil.clearAttachments(task);
        } catch (Exception e) {
            log.error("clear attachments error", e);
        }

        if (task.isCanceled()) {
            log.warn("test task {} is canceled, no data will be saved", task.getId());
            return;
        }

        if (webSocketCallback != null) {
            webSocketCallback.onTaskComplete(task);
        }

        log.info("test task {} is completed, start to save info", task.getId());

        testDataService.saveTestTaskData(task);
        runningTestTask.remove(task.getId());
    }

    @Override
    public void onOneDeviceComplete(Task task, TestRunDevice testRunDevice, Logger logger, TestRun result) {
        log.info("onOneDeviceComplete: {}", testRunDevice.getDeviceInfo().getSerialNum());
        testRunDeviceOrchestrator.finishTask(testRunDevice);
        //check if the device is needed to reboot
        testRunDeviceOrchestrator.rebootDeviceIfNeeded(testRunDevice, logger);
        File deviceTestResultFolder = result.getResultFolder();

        File[] files = deviceTestResultFolder.listFiles();
        List<StorageFileInfo> attachments = new ArrayList<>();
        Assert.notNull(files, "should have result file to upload");
        for (File file : files) {
            if (!file.isDirectory()) {
                attachments.add(saveFileToBlob(file, deviceTestResultFolder, logger));
            } else if (file.listFiles().length > 0) {
                File zipFile = FileUtil.zipFile(file.getAbsolutePath(),
                        deviceTestResultFolder + "/" + file.getName() + ".zip");
                attachments.add(saveFileToBlob(zipFile, deviceTestResultFolder, logger));
            }
        }
        result.setAttachments(attachments);
        result.processAndSaveDeviceTestResultBlobUrl();
    }

    @Override
    public void onDeviceOffline(Task task) {
        task.setStatus(Task.TaskStatus.CANCELED);
        if (webSocketCallback != null) {
            webSocketCallback.onDeviceOffline(task);
        }
        log.warn("device disconnected, test task {} will be re-queue, no data will be saved", task.getId());
    }

    public StorageFileInfo saveFileToBlob(File file, File folder, Logger logger) {
        StorageFileInfo storageFileInfo = new StorageFileInfo(file,
                "test/result/" + folder.getParentFile().getName() + "/" + folder.getName(),
                StorageFileInfo.FileType.COMMON_FILE);
        return attachmentService.saveFileInStorageAndDB(storageFileInfo, file, EntityType.TEST_RESULT, logger);
    }
}
