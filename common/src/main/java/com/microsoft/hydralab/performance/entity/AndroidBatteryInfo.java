// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class AndroidBatteryInfo implements Serializable {
    private String appPackageName;
    private long timeStamp;
    private float cpu;
    private float screen;
    private float wakeLock;
    private float systemService;
    private float wifi;
    private float appUsage;
    private float total;
    private float ratio;
    private String description;
}
