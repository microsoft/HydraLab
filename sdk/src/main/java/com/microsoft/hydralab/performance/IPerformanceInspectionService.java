package com.microsoft.hydralab.performance;

import java.util.List;

public interface IPerformanceInspectionService {
    void initialize(PerformanceTestSpec performanceTestSpec);
    List<PerformanceInspectionResult> inspect(PerformanceTestSpec performanceTestSpec);
    PerformanceTestResult parse(PerformanceTestSpec performanceTestSpec);
}
