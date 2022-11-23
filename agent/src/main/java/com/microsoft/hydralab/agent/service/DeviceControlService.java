// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.service;

import com.microsoft.hydralab.agent.repository.MobileDeviceRepository;
import com.microsoft.hydralab.agent.runner.RunningControlService;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.common.entity.agent.MobileDevice;
import com.microsoft.hydralab.common.entity.agent.RunningControl;
import com.microsoft.hydralab.common.entity.center.AgentUser;
import com.microsoft.hydralab.common.entity.center.TestTaskSpec;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.Message;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.management.impl.WindowsDeviceManager;
import com.microsoft.hydralab.common.util.Const;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Service
public class DeviceControlService {

    static final Logger log = LoggerFactory.getLogger(DeviceControlService.class);
    @Resource
    DeviceManager deviceManager;
    @Resource
    MobileDeviceRepository mobileDeviceRepository;
    @Resource
    AgentWebSocketClientService agentWebSocketClientService;
    @Resource
    RunningControlService runningControlService;
    @Resource
    ApplicationContext applicationContext;

    public Set<DeviceInfo> getAllConnectedDevice() {
        updateAllDeviceScope();
        return deviceManager.getDeviceList(log);
    }

    public void provideDeviceList(AgentUser.BatteryStrategy batteryStrategy) {
        captureAllScreensSync(batteryStrategy);
        Set<DeviceInfo> allConnectedDevices = getAllConnectedDevice();
        ArrayList<DeviceInfo> deviceInfos = new ArrayList<>(allConnectedDevices);
        deviceInfos.sort(Comparator.comparing(d -> d.getName() + d.getSerialNum()));
        Message message = new Message();
        message.setPath(Const.Path.DEVICE_LIST);
        message.setBody(deviceInfos);
        agentWebSocketClientService.send(message);
        log.info("/api/device/list device SN: {}", deviceInfos.stream().map(MobileDevice::getSerialNum).collect(Collectors.joining(",")));
    }

    public void captureAllScreensSync(AgentUser.BatteryStrategy batteryStrategy) {
        Set<DeviceInfo> allConnectedDevices = deviceManager.getActiveDeviceList(log);
        if (deviceManager instanceof WindowsDeviceManager) {
            Assert.isTrue(allConnectedDevices.size() == 1, "expect only 1 device");
        }
        captureDevicesScreenSync(allConnectedDevices, false, batteryStrategy);
    }

    private void captureDevicesScreenSync(Collection<DeviceInfo> allDevices, boolean logging, AgentUser.BatteryStrategy batteryStrategy) {
        RunningControl runningControl = runningControlService.runForAllDeviceAsync(allDevices, (deviceInfo, logger) -> {
            deviceManager.getScreenShotWithStrategy(deviceInfo, log, batteryStrategy);
            return true;
        }, null, logging, true);

        if (runningControl == null) {
            return;
        }

        CountDownLatch countDownLatch = runningControl.countDownLatch;
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }


    public boolean cancelTestTaskById(String testId) {
        final Map<String, TestTask> runningTestTask = deviceManager.getRunningTestTask();
        final TestTask testTask = runningTestTask.get(testId);
        if (testTask == null || testTask.isCanceled()) {
            return false;
        }
        testTask.setStatus(TestTask.TestStatus.CANCELED);
        deviceManager.resetDeviceByTestId(testId, log);
        return true;
    }


    private void updateAllDeviceScope() {
        List<MobileDevice> devices = mobileDeviceRepository.findAll();
        for (MobileDevice device : devices) {
            deviceManager.updateIsPrivateByDeviceSerial(device.getSerialNum(), device.getIsPrivate());
        }
    }

    public DeviceInfo updateDeviceScope(String deviceSerial, Boolean isPrivate) {
        Set<DeviceInfo> allActiveConnectedDevice = deviceManager.getActiveDeviceList(log);
        List<DeviceInfo> devices = allActiveConnectedDevice.stream()
                .filter(adbDeviceInfo -> deviceSerial.equals(adbDeviceInfo.getSerialNum()))
                .collect(Collectors.toList());
        if (devices.size() != 1) {
            throw new RuntimeException("Device " + deviceSerial + " not connected!");
        }
        DeviceInfo device = devices.get(0);
        device.setIsPrivate(isPrivate);
        if (mobileDeviceRepository.countBySerialNum(deviceSerial) > 0) {
            MobileDevice savedDevice = mobileDeviceRepository.getFirstBySerialNum(deviceSerial);
            savedDevice.setIsPrivate(isPrivate);
            mobileDeviceRepository.save(savedDevice);
        } else {
            MobileDevice deviceCopy = new MobileDevice();
            deviceCopy.setSerialNum(device.getSerialNum());
            deviceCopy.setName(device.getName());
            deviceCopy.setManufacturer(device.getManufacturer());
            deviceCopy.setModel(device.getModel());
            deviceCopy.setOsVersion(device.getOsVersion());
            deviceCopy.setScreenSize(device.getScreenSize());
            deviceCopy.setScreenDensity(device.getScreenDensity());
            deviceCopy.setOsSDKInt(device.getOsSDKInt());
            deviceCopy.setIsPrivate(isPrivate);

            mobileDeviceRepository.save(deviceCopy);
            deviceManager.updateIsPrivateByDeviceSerial(deviceSerial, isPrivate);
        }
        return device;
    }

    public TestTask runTestTask(TestTaskSpec testTaskSpec) {
        if (testTaskSpec.instrumentationArgs != null) {
            log.info("instrumentationArgs: {}", testTaskSpec.instrumentationArgs);
        }

        if (StringUtils.isEmpty(testTaskSpec.runningType)) {
            testTaskSpec.runningType = TestTask.TestRunningType.INSTRUMENTATION;
        }
        determineScopeOfTestCase(testTaskSpec);
        log.info("TestTaskSpec: {}", testTaskSpec);

        String beanName = TestTask.TestRunnerMap.get(testTaskSpec.runningType);
        TestRunner runner = applicationContext.getBean(beanName, TestRunner.class);
        return runner.runTest(testTaskSpec);
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

    public DeviceManager getDeviceManager() {
        return deviceManager;
    }
}
