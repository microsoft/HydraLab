// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhoule
 * @date 03/10/2023
 */

public class TestRunDeviceCombo extends TestRunDevice {
    Map<String, Integer> deviceCountMap = new HashMap<>();

    private List<TestRunDevice> pairedDevices = new ArrayList<>();

    public TestRunDeviceCombo(@NotNull DeviceInfo mainDeviceInfo, @NotNull List<DeviceInfo> deviceInfos) {
        super(mainDeviceInfo, mainDeviceInfo.getType() + "_" + 0);
        deviceCountMap.put(mainDeviceInfo.getType(), 1);
        for (DeviceInfo deviceInfo : deviceInfos) {
            pairedDevices.add(new TestRunDevice(deviceInfo, deviceInfo.getType() + "_" + deviceCountMap.getOrDefault(deviceInfo.getType(), 0)));
            deviceCountMap.put(deviceInfo.getType(), deviceCountMap.getOrDefault(deviceInfo.getType(), 0) + 1);
        }
    }

    public DeviceInfo getDeviceByTag(String tag) {
        if (super.getTag().equals(tag)) {
            return super.getDeviceInfo();
        }
        for (TestRunDevice device : pairedDevices) {
            if (device.getTag().equals(tag)) {
                return device.getDeviceInfo();
            }
        }
        return null;
    }

    public List<TestRunDevice> getPairedDevices() {
        return pairedDevices;
    }
    public List<TestRunDevice> getDevices() {
        List<TestRunDevice> devices = new ArrayList<>();
        devices.addAll(pairedDevices);
        devices.add(this);
        return devices;
    }
}
