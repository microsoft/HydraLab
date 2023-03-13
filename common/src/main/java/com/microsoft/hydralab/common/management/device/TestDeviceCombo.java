// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.management.device;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhoule
 * @date 03/10/2023
 */

public class TestDeviceCombo extends TestDevice{

    private List<TestDevice> pairedDevices = new ArrayList<>();

    public TestDeviceCombo(DeviceInfo deviceInfo, TestDeviceTag tag) {
        super(deviceInfo, tag);
    }

    public DeviceInfo getDeviceByTag(String tag) {
        if(super.getTag().equals(tag)) {
            return super.getDeviceInfo();
        }
        for (TestDevice device : pairedDevices) {
            if (device.getTag().equals(tag)) {
                return device.getDeviceInfo();
            }
        }
        return null;
    }
}
