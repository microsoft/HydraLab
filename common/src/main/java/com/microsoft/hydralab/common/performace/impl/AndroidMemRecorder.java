// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.performace.impl;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.performance.PerfMetaInfo;

/**
 * @author zhoule
 * @date 12/14/2022
 */

public class AndroidMemRecorder extends BasePerformanceRecorder {

    public AndroidMemRecorder(DeviceInfo deviceInfo, String resultsDir) {
        super(deviceInfo, resultsDir);
    }

    @Override
    public void initDevice() {
        // init device
    }

    @Override
    public void addMetricsData(PerfMetaInfo perfMetaInfo) {
        // add ..
    }

    @Override
    public void analyzeResult() {
        // analysis
    }
}
