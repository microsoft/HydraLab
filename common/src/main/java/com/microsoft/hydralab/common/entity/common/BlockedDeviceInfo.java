// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@ToString
@Entity
@Table
public class BlockedDeviceInfo implements Serializable {
    @Id
    @Column(name = "blocked_device_serial_number", nullable = false)
    public String blockedDeviceSerialNumber;
    @Column(name = "blocked_time", nullable = false)
    public Instant blockedTime;
    @Column(name = "blocking_task_uuid", nullable = false)
    public String blockingTaskUUID;
}
