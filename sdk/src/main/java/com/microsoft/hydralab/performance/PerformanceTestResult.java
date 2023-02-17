// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import java.util.ArrayList;
import java.util.List;

public class PerformanceTestResult {
    /**
     * memory: java_heap_pss	java_heap_rss	native_heap_pss	native_heap_rss	code_pss	code_rss	stack_pss	stack_rss
     * graphics_pss	graphics_rss	private_other_pss	private_other_rss	system_pss	system_rss	unknown_pss	unknown_rss
     * total_pss	total_rss	total_swap_pss
     * <p>
     * battery: CPU	screen	Wake_lock	other	App_usage	Total_usage
     */
    private Object resultSummary;
    /**
     * TODO: Apply a max size to avoid OOM
     */
    public List<PerformanceInspectionResult> performanceInspectionResults = new ArrayList<>();
    public PerformanceInspector.PerformanceInspectorType inspectorType;
    public PerformanceResultParser.PerformanceResultParserType parserType;

    //TODO: overwrite equals, toString, and hashcode methods


    public void setResultSummary(Object resultSummary) {
        //TODO: restrict the usage
        this.resultSummary = resultSummary;
    }

    public Object getResultSummary() {
        return resultSummary;
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
