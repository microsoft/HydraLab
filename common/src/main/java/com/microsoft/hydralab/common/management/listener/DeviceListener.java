// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.management.listener;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;

public interface DeviceListener {
    void onDeviceInactive(DeviceInfo deviceInfo);

    void onDeviceConnected(DeviceInfo deviceInfo);
}
