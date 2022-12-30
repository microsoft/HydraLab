// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import java.util.List;

public interface PerformanceInspector {
    void initialize(PerformanceInspection performanceInspection);

    PerformanceInspectionResult inspect(PerformanceInspection performanceInspection);

    PerformanceTestResult parse(List<PerformanceInspectionResult> performanceInspectionResultList);
}
