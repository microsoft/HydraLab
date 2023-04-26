// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.management.listener;

import com.android.ddmlib.IDevice;

public enum MobileDeviceState {
    // Device status
    OFFLINE,
    ONLINE,
    DISCONNECTED,
    TESTING,
    UNSTABLE,
    OTHER;

    public static MobileDeviceState mobileDeviceStateMapping(IDevice.DeviceState adbState) {
        if (adbState == null) {
            return OTHER;
        }

        switch (adbState) {
            case ONLINE:
                return ONLINE;
            case OFFLINE:
                return OFFLINE;
            case DISCONNECTED:
                return DISCONNECTED;
            default:
                return OTHER;
        }
    }
}