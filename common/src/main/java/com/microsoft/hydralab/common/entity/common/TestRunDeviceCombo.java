// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhoule
 * @date 03/10/2023
 */

public class TestRunDeviceCombo extends TestRunDevice {

    private List<TestRunDevice> pairedDevices = new ArrayList<>();

    public TestRunDeviceCombo(DeviceInfo deviceInfo, String tag) {
        super(deviceInfo, tag);
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
