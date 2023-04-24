// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.parsers;

import com.microsoft.hydralab.performance.PerformanceResultParser;
import com.microsoft.hydralab.performance.PerformanceTestResult;

/**
 * @author taoran
 * @date 4/17/2023
 */

public class EventTimeResultParser implements PerformanceResultParser {

    @Override
    public PerformanceTestResult parse(PerformanceTestResult performanceTestResult) {
        return performanceTestResult;
    }
}
