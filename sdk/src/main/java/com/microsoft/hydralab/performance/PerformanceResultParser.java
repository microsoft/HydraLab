package com.microsoft.hydralab.performance;

import java.util.List;

public interface PerformanceResultParser {
    enum PerformanceResultParserName {
        PARSER_ANDROID_MEMORY_DUMP,
        PARSER_ANDROID_MEMORY_INFO,
        PARSER_ANDROID_BATTERY_INFO,
        PARSER_WIN_BATTERY,
        PARSER_WIN_MEMORY
    }

    PerformanceTestResult parse(List<PerformanceInspectionResult> performanceInspectionResultList);
}
