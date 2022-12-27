// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.performace.impl;

import com.microsoft.hydralab.performance.PerformanceInspector;
import com.microsoft.hydralab.performance.PerformanceResult;
import com.microsoft.hydralab.performance.PerformanceTestSpec;

/**
 * @author zhoule
 * @date 12/14/2022
 */

public class AndroidMemoryInspector implements PerformanceInspector {

    @Override
    public void initialize(PerformanceTestSpec performanceTestSpec) {

    }

    @Override
    public void capturePerformanceMetrics(PerformanceTestSpec performanceTestSpec) {

    }

    @Override
    public PerformanceResult<?> analyzeResults(PerformanceTestSpec performanceTestSpec) {
        return null;
    }
}
