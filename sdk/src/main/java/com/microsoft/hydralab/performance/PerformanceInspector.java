// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

/**
 * @author zhoule
 * @date 12/14/2022
 */

public interface PerformanceInspector {
    void initDevice(PerfMetaInfo perfMetaInfo);

    void addMetricsData(PerfMetaInfo perfMetaInfo);

    PerfResult<?> analyzeResult(PerfMetaInfo perfMetaInfo);
}
