package com.microsoft.hydralab.performance;

public interface IPerformanceInspectionService {
    PerformanceInspectionResult inspect(PerformanceInspection performanceInspection);
    void inspectWithStrategy(PerformanceInspection performanceInspection, InspectionStrategy inspectionStrategy);
}
