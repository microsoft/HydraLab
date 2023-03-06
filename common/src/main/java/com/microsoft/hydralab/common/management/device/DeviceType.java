// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.management.device;

public enum DeviceType {
    // Define device type and bean name
    ANDROID("androidDeviceManager"),
    WINDOWS("windowsDeviceManager"),
    IOS("iosDeviceManager");
    String beanName;

    DeviceType(String beanName) {
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }
}