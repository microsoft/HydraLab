// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

/**
 * @author zhoule
 * @date 03/02/2023
 */

public class DeviceCombo extends DeviceInfo {

    DeviceInfo linkedDeviceInfo;

    public DeviceCombo(DeviceInfo deviceInfo, DeviceInfo linkedDeviceInfo) {
        super(deviceInfo);
        this.linkedDeviceInfo = linkedDeviceInfo;
    }

    public DeviceInfo getLinkedDeviceInfo() {
        return linkedDeviceInfo;
    }
}
