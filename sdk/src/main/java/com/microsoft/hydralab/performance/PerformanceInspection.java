package com.microsoft.hydralab.performance;

import java.io.File;

public class PerformanceInspection {
    public final String inspector;
    public final String appId;
    public final String deviceIdentifier;
    public final String name;
    public final String inspectionKey;
    File resultFolder;

    public PerformanceInspection(String name, String inspector, String appId, String deviceIdentifier) {
        this.inspector = inspector;
        this.appId = appId;
        this.deviceIdentifier = deviceIdentifier;
        this.name = name;
        inspectionKey = String.format("%s-%s-%s", appId, deviceIdentifier, inspector);
    }

    public static PerformanceInspection createAndroidBatteryInfoSpec(String appId, String deviceIdentifier) {
        return new PerformanceInspection(getNameByParam(PerformanceInspector.INSPECTOR_ANDROID_BATTERY_INFO, appId, deviceIdentifier),
                PerformanceInspector.INSPECTOR_ANDROID_BATTERY_INFO, appId, deviceIdentifier);
    }

    private static String getNameByParam(String inspector, String appId, String deviceId) {
        return String.format("PerfTesting: get %s for %s on %s", inspector, appId, deviceId);
    }
}
