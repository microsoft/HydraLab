// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.performace.impl;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.performance.PerformanceRecorder;

import java.io.File;
import java.util.UUID;

/**
 * @author zhoule
 * @date 12/14/2022
 */

public class AndroidMemRecorder implements PerformanceRecorder {
    DeviceInfo deviceInfo;
    String fileName = "";

    public AndroidMemRecorder(DeviceInfo deviceInfo, File file) {
        this.deviceInfo = deviceInfo;
        fileName = UUID.randomUUID().toString();
    }

    @Override
    public void beforeTest() {
        // init device
    }

    @Override
    public void addRecord() {
        // add ..
        
    }

    @Override
    public void afterTest() {
        // analysis
    }
}
