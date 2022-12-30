package com.microsoft.hydralab.performance;

import java.util.List;

public interface IPerformanceInspectionService {
    void initialize(PerformanceInspection performanceInspection);
    List<PerformanceInspectionResult> inspect(PerformanceInspection performanceInspection);
    PerformanceTestResult parse(PerformanceInspection performanceInspection);
}
