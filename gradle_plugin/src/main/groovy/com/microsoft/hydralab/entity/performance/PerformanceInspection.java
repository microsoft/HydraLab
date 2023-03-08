// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.entity.performance;

import java.io.File;
import java.io.Serializable;

import static com.microsoft.hydralab.entity.performance.PerformanceInspector.PerformanceInspectorType.INSPECTOR_ANDROID_BATTERY_INFO;
import static com.microsoft.hydralab.entity.performance.PerformanceInspector.PerformanceInspectorType.INSPECTOR_ANDROID_MEMORY_INFO;
import static com.microsoft.hydralab.entity.performance.PerformanceInspector.PerformanceInspectorType.INSPECTOR_WIN_BATTERY;
import static com.microsoft.hydralab.entity.performance.PerformanceInspector.PerformanceInspectorType.INSPECTOR_WIN_MEMORY;

public class PerformanceInspection implements Serializable {

    public PerformanceInspector.PerformanceInspectorType inspectorType;
    public String appId;
    public String deviceIdentifier = "";
    public String description;
    public String inspectionKey = "";
    public boolean isReset = false;
    public File resultFolder;
}
