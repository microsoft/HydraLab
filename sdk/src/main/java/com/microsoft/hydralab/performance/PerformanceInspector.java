package com.microsoft.hydralab.performance;

public interface PerformanceInspector {
    String INSPECTOR_ANDROID_MEMORY_DUMP = "AndroidMemoryDump";
    String INSPECTOR_ANDROID_MEMORY_INFO = "AndroidMemoryInfo";
    String INSPECTOR_ANDROID_BATTERY_INFO = "AndroidBatteryInfo";
    String INSPECTOR_WIN_BATTERY = "WindowsBattery";
    String INSPECTOR_WIN_MEMORY = "WindowsMemory";
    PerformanceInspectionResult inspect(PerformanceInspection performanceInspection);
}
