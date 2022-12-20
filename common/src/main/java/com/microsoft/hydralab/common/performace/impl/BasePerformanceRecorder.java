package com.microsoft.hydralab.common.performace.impl;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.performance.PerformanceRecorder;

import java.io.File;

public abstract class BasePerformanceRecorder implements PerformanceRecorder {
    protected DeviceInfo deviceInfo;
    protected File resultsDir;

    BasePerformanceRecorder(DeviceInfo deviceInfo, String resultsDir) {
        this.deviceInfo = deviceInfo;
        this.resultsDir = new File(resultsDir);
    }
}
