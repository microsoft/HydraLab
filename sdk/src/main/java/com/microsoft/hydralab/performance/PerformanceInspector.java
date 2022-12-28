// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import java.io.File;
import java.util.List;

public interface PerformanceInspector {
    void initialize(PerformanceTestSpec performanceTestSpec, File resultFolder);

    PerformanceInspectionResult capturePerformanceMetrics(PerformanceTestSpec performanceTestSpec, File resultFolder);

    PerformanceResult<?> analyzeResults(List<PerformanceInspectionResult> performanceInspectionResultList);
}
