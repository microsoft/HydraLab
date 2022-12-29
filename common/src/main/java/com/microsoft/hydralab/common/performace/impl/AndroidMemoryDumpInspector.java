// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.performace.impl;

import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceInspector;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import com.microsoft.hydralab.performance.PerformanceTestSpec;

import java.util.List;

public class AndroidMemoryDumpInspector implements PerformanceInspector {

    @Override
    public void initialize(PerformanceTestSpec performanceTestSpec) {

    }

    @Override
    public PerformanceInspectionResult inspect(PerformanceTestSpec performanceTestSpec) {
        return null;
    }

    @Override
    public PerformanceTestResult parse(List<PerformanceInspectionResult> performanceInspectionResultList) {
        return null;
    }

}
