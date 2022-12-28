package com.microsoft.hydralab.common.performace.impl;

import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceInspector;
import com.microsoft.hydralab.performance.PerformanceResult;
import com.microsoft.hydralab.performance.PerformanceTestSpec;

import java.io.File;
import java.util.List;

public class WindowsBatteryInspector implements PerformanceInspector {
    @Override
    public void initialize(PerformanceTestSpec performanceTestSpec, File resultFolder) {
        
    }

    @Override
    public PerformanceInspectionResult inspect(PerformanceTestSpec performanceTestSpec, File resultFolder) {
        return null;
    }

    @Override
    public PerformanceResult<?> parse(List<PerformanceInspectionResult> performanceInspectionResultList) {
        return null;
    }


}
