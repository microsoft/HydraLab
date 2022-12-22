// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.performace.impl;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.performance.PerfMetaInfo;

/**
 * @author zhoule
 * @date 12/15/2022
 */

public class AndroidBatteryInspector extends BasePerformanceInspector {

    AndroidBatteryInspector(DeviceInfo deviceInfo, String resultsDir) {
        super(deviceInfo, resultsDir);
    }

    @Override
    public void initDevice() {

    }

    @Override
    public void addMetricsData(PerfMetaInfo perfMetaInfo) {

    }


    @Override
    public void analyzeResult() {

    }
}
