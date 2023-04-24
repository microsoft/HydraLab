// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.inspectors;

import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceInspector;

/**
 * @author taoran
 * @date 4/17/2023
 */

public class EventTimeInspector implements PerformanceInspector {
    @Override
    public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
        return new PerformanceInspectionResult(null, performanceInspection);
    }
}
