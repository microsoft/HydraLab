// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import java.util.List;

public interface PerformanceInspector {
    void initialize(PerformanceTestSpec performanceTestSpec);

    PerformanceInspectionResult inspect(PerformanceTestSpec performanceTestSpec);

    PerformanceTestResult parse(List<PerformanceInspectionResult> performanceInspectionResultList);
}
