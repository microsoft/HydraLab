package com.microsoft.hydralab.common.performace.impl;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.performance.PerformanceInspector;

import java.io.File;

public abstract class BasePerformanceInspector implements PerformanceInspector {
    protected DeviceInfo deviceInfo;
    protected File resultsDir;

    public BasePerformanceInspector(DeviceInfo deviceInfo, String resultsDir) {
        this.deviceInfo = deviceInfo;
        this.resultsDir = new File(resultsDir);
    }
}
