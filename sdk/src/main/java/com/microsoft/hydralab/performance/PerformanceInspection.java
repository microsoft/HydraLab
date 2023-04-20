// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance;

import java.io.File;
import java.io.Serializable;

import static com.microsoft.hydralab.performance.PerformanceInspector.PerformanceInspectorType.INSPECTOR_ANDROID_BATTERY_INFO;
import static com.microsoft.hydralab.performance.PerformanceInspector.PerformanceInspectorType.INSPECTOR_ANDROID_MEMORY_INFO;
import static com.microsoft.hydralab.performance.PerformanceInspector.PerformanceInspectorType.INSPECTOR_IOS_ENERGY;
import static com.microsoft.hydralab.performance.PerformanceInspector.PerformanceInspectorType.INSPECTOR_IOS_MEMORY;
import static com.microsoft.hydralab.performance.PerformanceInspector.PerformanceInspectorType.INSPECTOR_WIN_BATTERY;
import static com.microsoft.hydralab.performance.PerformanceInspector.PerformanceInspectorType.INSPECTOR_WIN_MEMORY;

public class PerformanceInspection implements Serializable {

    @SuppressWarnings("visibilitymodifier")
    public final PerformanceInspector.PerformanceInspectorType inspectorType;
    @SuppressWarnings("visibilitymodifier")
    public final String appId;
    @SuppressWarnings("visibilitymodifier")
    public final String deviceIdentifier;
    @SuppressWarnings("visibilitymodifier")
    public final String description;
    @SuppressWarnings("visibilitymodifier")
    public final String inspectionKey;
    @SuppressWarnings("visibilitymodifier")
    public final boolean isReset;
    @SuppressWarnings("visibilitymodifier")
    public File resultFolder;

    public PerformanceInspection(String description, PerformanceInspector.PerformanceInspectorType inspectorType, String appId, String deviceIdentifier, boolean isReset) {
        this.inspectorType = inspectorType;
        this.appId = appId;
        this.deviceIdentifier = deviceIdentifier;
        this.description = description;
        this.isReset = isReset;
        inspectionKey = String.format("%s-%s-%s", appId, deviceIdentifier, inspectorType);
    }

    public static PerformanceInspection createAndroidBatteryInfoInspection(String appId, String deviceIdentifier, String description) {
        return createAndroidBatteryInfoInspection(appId, deviceIdentifier, description, false);
    }

    public static PerformanceInspection createWindowsBatteryInspection(String appId, String deviceIdentifier, String description) {
        return createWindowsBatteryInspection(appId, deviceIdentifier, description, false);
    }

    public static PerformanceInspection createWindowsMemoryInspection(String appId, String deviceIdentifier, String description) {
        return createWindowsMemoryInspection(appId, deviceIdentifier, description, false);
    }

    public static PerformanceInspection createAndroidMemoryInfoInspection(String appId, String deviceIdentifier, String description, boolean isReset) {
        return new PerformanceInspection(description, INSPECTOR_ANDROID_MEMORY_INFO, appId, deviceIdentifier, isReset);
    }

    public static PerformanceInspection createAndroidBatteryInfoInspection(String appId, String deviceIdentifier, String description, boolean isReset) {
        return new PerformanceInspection(description, INSPECTOR_ANDROID_BATTERY_INFO, appId, deviceIdentifier, isReset);
    }

    public static PerformanceInspection createWindowsBatteryInspection(String appId, String deviceIdentifier, String description, boolean isReset) {
        return new PerformanceInspection(description, INSPECTOR_WIN_BATTERY, appId, deviceIdentifier, isReset);
    }

    public static PerformanceInspection createWindowsMemoryInspection(String appId, String deviceIdentifier, String description, boolean isReset) {
        return new PerformanceInspection(description, INSPECTOR_WIN_MEMORY, appId, deviceIdentifier, isReset);
    }

    public static PerformanceInspection createIOSEnergyInspection(String appId, String deviceIdentifier, String description, boolean isReset) {
        return new PerformanceInspection(description, INSPECTOR_IOS_ENERGY, appId, deviceIdentifier, isReset);
    }

    public static PerformanceInspection createIOSMemoryInspection(String appId, String deviceIdentifier, String description, boolean isReset) {
        return new PerformanceInspection(description, INSPECTOR_IOS_MEMORY, appId, deviceIdentifier, isReset);
    }

    public static PerformanceInspection createEventStartInspection(String deviceIdentifier, String description) {
        return new PerformanceInspection(description, PerformanceInspector.PerformanceInspectorType.INSPECTOR_EVENT_TIME, null, deviceIdentifier, true);
    }

    public static PerformanceInspection createEventEndInspection(String deviceIdentifier, String description) {
        return new PerformanceInspection(description, PerformanceInspector.PerformanceInspectorType.INSPECTOR_EVENT_TIME, null, deviceIdentifier, false);
    }
}
