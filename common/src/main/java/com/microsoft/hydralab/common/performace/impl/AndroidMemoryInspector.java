// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.performace.impl;

import com.microsoft.hydralab.performance.PerfResult;
import com.microsoft.hydralab.performance.PerformanceInspector;
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
    public void capturePerformanceMatrix(PerformanceTestSpec performanceTestSpec) {

    }

    @Override
    public PerfResult<?> analyzeResult(PerformanceTestSpec performanceTestSpec) {
        return null;
    }
}
