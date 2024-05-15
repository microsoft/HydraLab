// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.parsers;

import com.microsoft.hydralab.performance.PerformanceResultParser;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import org.slf4j.Logger;

/**
 * @author taoran
 * @date 4/17/2023
 */

public class EventTimeResultParser implements PerformanceResultParser {

    @Override
    public PerformanceTestResult parse(PerformanceTestResult performanceTestResult, Logger logger) {
        return performanceTestResult;
    }
}
