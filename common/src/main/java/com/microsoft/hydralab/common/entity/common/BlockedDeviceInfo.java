// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@ToString
public class BlockedDeviceInfo {
    public Instant blockedTime;
    public String blockingTaskUUID;
    public String blockedDeviceSerialNumber;
}
