package com.microsoft.hydralab.performance;

import java.util.List;

public class PerformanceResult<T> {
    String category;

    /**
     * memory: java_heap_pss	java_heap_rss	native_heap_pss	native_heap_rss	code_pss	code_rss	stack_pss	stack_rss
     * graphics_pss	graphics_rss	private_other_pss	private_other_rss	system_pss	system_rss	unknown_pss	unknown_rss
     * total_pss	total_rss	total_swap_pss
     * <p>
     * battery: CPU	screen	Wake_lock	other	App_usage	Total_usage
     */
    T performanceData;
    List<PerformanceInspectionResult> performanceInspectionResultList;
}
