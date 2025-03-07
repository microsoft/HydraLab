// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

/**
 * @author zhoule
 * @date 03/06/2025
 */

@Data
public class DeviceOperation {
    String deviceSerial;
    OperationType operationType;
    String fromPositionX;
    String fromPositionY;
    String toPositionX;
    String toPositionY;

    public enum OperationType {
        SWIPE,
        TAP,
        REBOOT,
        WAKEUP
    }
}

