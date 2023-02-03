package com.microsoft.hydralab.performance;

public interface PerformanceInspector {
    enum PerformanceInspectorType {
        INSPECTOR_ANDROID_MEMORY_DUMP,
        INSPECTOR_ANDROID_MEMORY_INFO,
        INSPECTOR_ANDROID_BATTERY_INFO,
        INSPECTOR_WIN_MEMORY,
        INSPECTOR_WIN_BATTERY
    }

    PerformanceInspectionResult inspect(PerformanceInspection performanceInspection);
}
