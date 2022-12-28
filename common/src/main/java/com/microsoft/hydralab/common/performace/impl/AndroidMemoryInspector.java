// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.performace.impl;

import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceInspector;
import com.microsoft.hydralab.performance.PerformanceResult;
import com.microsoft.hydralab.performance.PerformanceTestSpec;

import java.io.File;
import java.util.List;

public class AndroidMemoryInspector implements PerformanceInspector {

    @Override
    public void initialize(PerformanceTestSpec performanceTestSpec, File resultFolder) {

    }

    @Override
    public PerformanceInspectionResult capturePerformanceMetrics(PerformanceTestSpec performanceTestSpec, File resultFolder) {
        return null;
    }

    @Override
    public PerformanceResult<?> analyzeResults(List<PerformanceInspectionResult> performanceInspectionResultList) {
        return null;
    }

}
