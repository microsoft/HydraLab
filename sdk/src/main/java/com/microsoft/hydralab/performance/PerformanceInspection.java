package com.microsoft.hydralab.performance;

import java.io.File;
import java.util.UUID;

public class PerformanceInspection {
    public static final String INSPECTOR_ANDROID_MEMORY_DUMP = "AndroidMemoryDump";
    public static final String INSPECTOR_ANDROID_MEMORY_INFO = "AndroidMemoryInfo";
    public static final String INSPECTOR_ANDROID_BATTERY_INFO = "AndroidBatteryInfo";
    public static final String INSPECTOR_WIN_BATTERY = "WindowsBattery";
    public static final String INSPECTOR_WIN_MEMORY = "WindowsMemory";
    public String id = UUID.randomUUID().toString();
    public final String inspector;
    public final String appId;
    public final String deviceId;
    public String name;
    public final String inspectionKey;
    public File resultFolder;


    public PerformanceInspection(String inspector, String appId, String deviceId, String name) {
        this.inspector = inspector;
        this.appId = appId;
        this.deviceId = deviceId;
        this.name = name;
        inspectionKey = String.format("%s-%s-%s", appId, deviceId, inspector);
    }

    public static PerformanceInspection createAndroidMemoryDumpSpec(String appId, String deviceId) {
        return new PerformanceInspection(INSPECTOR_ANDROID_MEMORY_DUMP, appId, deviceId, getNameByParam(INSPECTOR_ANDROID_MEMORY_DUMP, appId, deviceId));
    }

    public static PerformanceInspection createAndroidMemoryInfoSpec(String appId, String deviceId) {
        return new PerformanceInspection(INSPECTOR_ANDROID_MEMORY_INFO, appId, deviceId, getNameByParam(INSPECTOR_ANDROID_MEMORY_INFO, appId, deviceId));
    }

    public static PerformanceInspection createAndroidBatteryInfoSpec(String appId, String deviceId) {
        return new PerformanceInspection(INSPECTOR_ANDROID_BATTERY_INFO, appId, deviceId, getNameByParam(INSPECTOR_ANDROID_BATTERY_INFO, appId, deviceId));
    }

    public static PerformanceInspection createWindowsBatteryInfoSpec(String appId, String deviceId) {
        return new PerformanceInspection(INSPECTOR_WIN_BATTERY, appId, deviceId, getNameByParam(INSPECTOR_WIN_BATTERY, appId, deviceId));
    }

    public static PerformanceInspection createWindowsMemoryInfoSpec(String appId, String deviceId) {
        return new PerformanceInspection(INSPECTOR_WIN_MEMORY, appId, deviceId, getNameByParam(INSPECTOR_WIN_MEMORY, appId, deviceId));
    }

    public PerformanceInspection rename(String name) {
        this.name = name;
        return this;
    }

    private static String getNameByParam(String inspector, String appId, String deviceId) {
        return String.format("PerfTesting: get %s for %s on %s", inspector, appId, deviceId);
    }

}
