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
    /**
     * memory: java_heap_pss    java_heap_rss    native_heap_pss    native_heap_rss    code_pss    code_rss    stack_pss    stack_rss
     * graphics_pss    graphics_rss    private_other_pss    private_other_rss    system_pss    system_rss    unknown_pss    unknown_rss
     * total_pss    total_rss    total_swap_pss
     * <p>
     * battery: CPU    screen    Wake_lock    other    App_usage   Total_usage
     */
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
