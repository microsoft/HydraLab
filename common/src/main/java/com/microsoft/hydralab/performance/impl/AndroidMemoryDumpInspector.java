// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance.impl;

import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceInspector;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import com.microsoft.hydralab.performance.PerformanceInspection;

import java.util.List;

public class AndroidMemoryDumpInspector implements PerformanceInspector {

    @Override
    public void initialize(PerformanceInspection performanceInspection) {

    }

    @Override
    public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
        return null;
    }

    @Override
    public PerformanceTestResult parse(List<PerformanceInspectionResult> performanceInspectionResultList) {
        return null;
    }

}
