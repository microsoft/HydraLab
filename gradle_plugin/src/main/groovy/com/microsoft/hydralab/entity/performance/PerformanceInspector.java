// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.entity.performance;

public interface PerformanceInspector {

    PerformanceInspectionResult inspect(PerformanceInspection performanceInspection);

    enum PerformanceInspectorType {
        INSPECTOR_ANDROID_MEMORY_DUMP,
        INSPECTOR_ANDROID_MEMORY_INFO,
        INSPECTOR_ANDROID_BATTERY_INFO,
        INSPECTOR_WIN_MEMORY,
        INSPECTOR_WIN_BATTERY,
        INSPECTOR_IOS_MEMORY,
        INSPECTOR_IOS_ENERGY,
        INSPECTOR_EVENT_TIME
    }

}
