// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PerformanceTestResult {
    /**
     * TODO: Apply a max size to avoid OOM
     */
    @SuppressWarnings("visibilitymodifier")
    public List<PerformanceInspectionResult> performanceInspectionResults = new CopyOnWriteArrayList<>();
    @SuppressWarnings("visibilitymodifier")
    public PerformanceInspector.PerformanceInspectorType inspectorType;
    @SuppressWarnings("visibilitymodifier")
    public PerformanceResultParser.PerformanceResultParserType parserType;
    @SuppressWarnings("visibilitymodifier")
    private Object resultSummary;

    //TODO: overwrite equals, toString, and hashcode methods

    public Object getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(Object resultSummary) {
        //TODO: restrict the usage
        this.resultSummary = resultSummary;
    }

    @Override
    public String toString() {
        return "PerformanceTestResult{" +
                "resultSummary=" + resultSummary +
                ", performanceInspectionResults=" + performanceInspectionResults +
                ", inspectorType=" + inspectorType +
                ", parserType=" + parserType +
                '}';
    }
}
