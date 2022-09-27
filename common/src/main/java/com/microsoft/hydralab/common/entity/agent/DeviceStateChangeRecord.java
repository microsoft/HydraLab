// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.agent;


import com.microsoft.hydralab.common.management.DeviceManager.MobileDeviceState;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class DeviceStateChangeRecord {
    /**
     * Recording the appearance of a latest {@link MobileDeviceState}.
     * time
     * state
     */
    private String serialNumber;
    private LocalDateTime time;
    private MobileDeviceState state;
    // value: Const.DeviceStability.*
    private String behaviour;

}
