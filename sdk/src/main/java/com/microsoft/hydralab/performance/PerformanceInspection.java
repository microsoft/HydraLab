package com.microsoft.hydralab.performance;

import java.io.File;

import static com.microsoft.hydralab.performance.PerformanceInspector.PerformanceInspectorType.INSPECTOR_ANDROID_BATTERY_INFO;
import static com.microsoft.hydralab.performance.PerformanceInspector.PerformanceInspectorType.INSPECTOR_WIN_BATTERY;

public class PerformanceInspection {

    public final PerformanceInspector.PerformanceInspectorType inspectorType;
    public final String appId;
    public final String deviceIdentifier;
    public final String description;
    public final String inspectionKey;
    public final boolean isReset;
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

    public static PerformanceInspection createAndroidBatteryInfoInspection(String appId, String deviceIdentifier, String description, boolean isReset) {
        return new PerformanceInspection(description, INSPECTOR_ANDROID_BATTERY_INFO, appId, deviceIdentifier, isReset);
    }

    public static PerformanceInspection createWindowsBatteryInspection(String appId, String deviceIdentifier, String description, boolean isReset) {
        return new PerformanceInspection(description, INSPECTOR_WIN_BATTERY, appId, deviceIdentifier, isReset);
    }
}
