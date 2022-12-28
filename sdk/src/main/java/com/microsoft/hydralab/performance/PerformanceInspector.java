// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import java.io.File;

/**
 * @author zhoule
 * @date 12/14/2022
 */

public interface PerformanceInspector {
    void initialize(PerformanceTestSpec performanceTestSpec, File resultFolder);

    void capturePerformanceMetrics(PerformanceTestSpec performanceTestSpec, File resultFolder);

    PerformanceResult<?> analyzeResults(File resultFolder);
}
