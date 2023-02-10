// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

public interface IPerformanceInspectionService {
    PerformanceInspectionResult inspect(PerformanceInspection performanceInspection);

    void inspectWithStrategy(PerformanceInspection performanceInspection, InspectionStrategy inspectionStrategy);

    PerformanceTestResult parse(PerformanceInspection performanceInspection);

}
