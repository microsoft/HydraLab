// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance.Entity;

import lombok.Data;

@Data
public class AndroidBatteryInfo {
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
