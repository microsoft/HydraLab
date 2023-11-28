// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.service;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.agent.repository.MobileDeviceRepository;
import com.microsoft.hydralab.common.entity.agent.MobileDevice;
import com.microsoft.hydralab.common.entity.common.AgentUser;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.Message;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.device.DeviceType;
import com.microsoft.hydralab.common.management.device.impl.DeviceDriverManager;
import com.microsoft.hydralab.common.management.listener.DeviceStatusListener;
import com.microsoft.hydralab.common.management.listener.DeviceStatusListenerManager;
import com.microsoft.hydralab.common.management.listener.impl.DeviceStabilityMonitor;
import com.microsoft.hydralab.common.management.listener.impl.PreInstallListener;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.ThreadPoolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeviceControlService {

    @SuppressWarnings("constantname")
    static final Logger log = LoggerFactory.getLogger(DeviceControlService.class);
    @Resource
    AgentManagementService agentManagementService;
    @Resource
    DeviceDriverManager deviceDriverManager;
    @Resource
    MobileDeviceRepository mobileDeviceRepository;
    @Resource
    AgentWebSocketClientService agentWebSocketClientService;
    @Resource
    DeviceStabilityMonitor deviceStabilityMonitor;
    @Resource
    DeviceStatusListenerManager deviceStatusListenerManager;

    public Set<DeviceInfo> getAllConnectedDevice() {
        updateAllDeviceScope();
        return agentManagementService.getDeviceList(log);
    }

    public void provideDeviceList(AgentUser.BatteryStrategy batteryStrategy) {
        captureAllScreen(batteryStrategy);
        Set<DeviceInfo> allConnectedDevices = getAllConnectedDevice();
        ArrayList<DeviceInfo> deviceInfos = new ArrayList<>(allConnectedDevices);
        deviceInfos.sort(Comparator.comparing(d -> d.getName() + d.getSerialNum()));
        Message message = new Message();
        message.setPath(Const.Path.DEVICE_LIST);
        message.setBody(deviceInfos);
        agentWebSocketClientService.send(message);
        log.info("/api/device/list device SN: {}",
                deviceInfos.stream().map(MobileDevice::getSerialNum).collect(Collectors.joining(",")));
    }

    public void captureAllScreen(AgentUser.BatteryStrategy batteryStrategy) {
        Set<DeviceInfo> allConnectedDevices = agentManagementService.getActiveDeviceList(log);
        // we need to do this in an async way, otherwise the process will be blocked if one device is not responding
        allConnectedDevices.forEach(deviceInfo -> {
            ThreadPoolUtil.SCREENSHOT_EXECUTOR.execute(() -> {
                try {
                    deviceDriverManager.getScreenShotWithStrategy(deviceInfo, log, batteryStrategy);
                } catch (Exception e) {
                    log.error("Failed to capture screenshot for device: {}", deviceInfo.getSerialNum(), e);
                }
            });
        });
    }

    private void updateAllDeviceScope() {
        List<MobileDevice> devices = mobileDeviceRepository.findAll();
        for (MobileDevice device : devices) {
            agentManagementService.updateIsPrivateByDeviceSerial(device.getSerialNum(), device.getIsPrivate());
        }
    }

    public DeviceInfo updateDeviceScope(String deviceSerial, Boolean isPrivate) {
        Set<DeviceInfo> allActiveConnectedDevice = agentManagementService.getActiveDeviceList(log);
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
            agentManagementService.updateIsPrivateByDeviceSerial(deviceSerial, isPrivate);
        }
        return device;
    }

    public void deviceDriverInit() {
        deviceStatusListenerManager.registerListener(new DeviceStatusListener() {

            @Override
            public void onDeviceInactive(DeviceInfo deviceInfo) {
                //send message to master to update device status
                JSONObject data = new JSONObject();
                data.put(Const.AgentConfig.SERIAL_PARAM, deviceInfo.getSerialNum());
                if (DeviceInfo.UNSTABLE.equals(deviceInfo.getStatus())) {
                    data.put(Const.AgentConfig.STATUS_PARAM, deviceInfo.getStatus());
                } else {
                    data.put(Const.AgentConfig.STATUS_PARAM, DeviceInfo.OFFLINE);
                }
                agentWebSocketClientService.send(Message.ok(Const.Path.DEVICE_STATUS, data));
            }

            @Override
            public void onDeviceConnected(DeviceInfo deviceInfo) {
                //send message to master to update device status
                JSONObject data = new JSONObject();
                data.put(Const.AgentConfig.SERIAL_PARAM, deviceInfo.getSerialNum());
                if (DeviceInfo.UNSTABLE.equals(deviceInfo.getStatus())) {
                    data.put(Const.AgentConfig.STATUS_PARAM, deviceInfo.getStatus());
                } else {
                    data.put(Const.AgentConfig.STATUS_PARAM, DeviceInfo.ONLINE);
                }
                agentWebSocketClientService.send(Message.ok(Const.Path.DEVICE_STATUS, data));
            }
        });
        deviceStatusListenerManager.registerListener(new PreInstallListener(agentManagementService, deviceDriverManager));
        deviceStatusListenerManager.registerListener(deviceStabilityMonitor);

        deviceDriverManager.init();
    }

    public void rebootDevices(DeviceType deviceType) {
        Assert.notNull(deviceType, "deviceType cannot be null");
        agentManagementService.getActiveDeviceList(log).stream().filter(deviceInfo -> deviceType.name().equals(deviceInfo.getType()))
                .forEach(deviceInfo -> {
                    try {
                        deviceDriverManager.rebootDeviceAsync(deviceInfo, log);
                    } catch (Exception e) {
                        log.error("Failed to reboot device: {}", deviceInfo.getSerialNum(), e);
                    }
                });
    }
}
