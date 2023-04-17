// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.parsers;

import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceResultParser;
import com.microsoft.hydralab.performance.PerformanceTestResult;

import java.util.List;

/**
 * @author taoran
 * @date 4/17/2023
 */

public class LatencyResultParser implements PerformanceResultParser {

    @Override
    public PerformanceTestResult parse(PerformanceTestResult performanceTestResult) {
        if (performanceTestResult == null || performanceTestResult.performanceInspectionResults == null
                || performanceTestResult.performanceInspectionResults.isEmpty()) {
            return null;
        }

        List<PerformanceInspectionResult> inspectionResults = performanceTestResult.performanceInspectionResults;
        long startTimeStamp = inspectionResults.get(0).timestamp;
        for (PerformanceInspectionResult inspectionResult : inspectionResults) {
            long latency = -1;
            if (inspectionResult.inspection.isReset) {
                // Latency start
                startTimeStamp = inspectionResult.timestamp;
            } else {
                // Latency end, record the latency
                latency = inspectionResult.timestamp - startTimeStamp;
            }

            inspectionResult.parsedData = latency;
        }

        //TODO: parse latency summary
        return performanceTestResult;
    }
}
