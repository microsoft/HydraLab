package com.microsoft.hydralab.performance;

import java.util.List;

public interface IPerformanceInspectionService {
    void reset(PerformanceInspection performanceInspection);

    PerformanceInspectionResult inspect(PerformanceInspection performanceInspection);

    void inspectWithStrategy(PerformanceInspection performanceInspection, InspectionStrategy inspectionStrategy);

    List<PerformanceTestResult> parse();
}
