// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance.impl;

import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceInspector;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import com.microsoft.hydralab.performance.PerformanceTestSpec;

import java.util.List;

public class AndroidBatteryInspector implements PerformanceInspector {

    @Override
    public void initialize(PerformanceTestSpec performanceTestSpec) {

    }

    @Override
    public PerformanceInspectionResult inspect(PerformanceTestSpec performanceTestSpec) {
        // else capture performance metrics
        return null;
    }

    @Override
    public PerformanceTestResult parse(List<PerformanceInspectionResult> performanceInspectionResultList) {
        return null;
    }
}
