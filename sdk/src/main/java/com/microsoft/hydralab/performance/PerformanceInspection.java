package com.microsoft.hydralab.performance;

import java.io.File;

public class PerformanceInspection {
    public final String inspector;
    public final String appId;
    public final String deviceIdentifier;
    public final String name;
    public final String inspectionKey;
    public final boolean isReset;
    File resultFolder;

    public PerformanceInspection(String name, String inspector, String appId, String deviceIdentifier, boolean isReset) {
        this.inspector = inspector;
        this.appId = appId;
        this.deviceIdentifier = deviceIdentifier;
        this.name = name;
        this.isReset = isReset;
        inspectionKey = String.format("%s-%s-%s", appId, deviceIdentifier, inspector);
    }

    private static String getNameByParam(String inspector, String appId, String deviceId) {
        return String.format("PerfTesting: get %s for %s on %s", inspector, appId, deviceId);
    }

    public static PerformanceInspection createAndroidBatteryInfoInspection(String appId, String deviceIdentifier) {
        return createAndroidBatteryInfoInspection(appId, deviceIdentifier, false);
    }

    public static PerformanceInspection createWindowsBatteryInspection(String appId, String deviceIdentifier) {
        return createWindowsBatteryInspection(appId, deviceIdentifier, false);
    }

    public static PerformanceInspection createAndroidBatteryInfoInspection(String appId, String deviceIdentifier, boolean isReset) {
        return new PerformanceInspection(getNameByParam(PerformanceInspector.INSPECTOR_ANDROID_BATTERY_INFO, appId, deviceIdentifier),
                PerformanceInspector.INSPECTOR_ANDROID_BATTERY_INFO, appId, deviceIdentifier, isReset);
    }

    public static PerformanceInspection createWindowsBatteryInspection(String appId, String deviceIdentifier, boolean isReset) {
        return new PerformanceInspection(getNameByParam(PerformanceInspector.INSPECTOR_WIN_BATTERY, appId, deviceIdentifier),
                PerformanceInspector.INSPECTOR_WIN_BATTERY, appId, deviceIdentifier, isReset);
    }
}
