// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.performace.impl;

import com.microsoft.hydralab.performance.PerformanceInspector;
import com.microsoft.hydralab.performance.PerformanceResult;
import com.microsoft.hydralab.performance.PerformanceTestSpec;

import java.io.File;

public class AndroidMemoryInspector implements PerformanceInspector {

    @Override
    public void initialize(PerformanceTestSpec performanceTestSpec, File resultFolder) {

    }

    @Override
    public void capturePerformanceMetrics(PerformanceTestSpec performanceTestSpec, File resultFolder) {

    }

    @Override
    public PerformanceResult<?> analyzeResults(File resultFolder) {
        return null;
    }
}
