package com.microsoft.hydralab.performance;

import java.util.List;

public interface PerformanceResultParser {
    String PARSER_ANDROID_MEMORY_DUMP = "AndroidMemoryDump";
    String PARSER_ANDROID_MEMORY_INFO = "AndroidMemoryInfo";
    String PARSER_ANDROID_BATTERY_INFO = "AndroidBatteryInfo";
    String PARSER_WIN_BATTERY = "WindowsBattery";
    String PARSER_WIN_MEMORY = "WindowsMemory";

    PerformanceTestResult parse(List<PerformanceInspectionResult> performanceInspectionResultList);
}
