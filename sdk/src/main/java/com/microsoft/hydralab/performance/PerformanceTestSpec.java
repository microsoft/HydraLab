package com.microsoft.hydralab.performance;

import java.io.File;
import java.util.UUID;

public class PerformanceTestSpec {
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
    /**
     * TODO: where to set a value for this field?
     */
    public File resultFolder;
    public long interval;

    public PerformanceTestSpec(String inspector, String appId, String deviceId, String name) {
        this.inspector = inspector;
        this.appId = appId;
        this.deviceId = deviceId;
        this.name = name;
        inspectionKey = String.format("%s-%s-%s", appId, deviceId, inspector);
    }

    public static PerformanceTestSpec createAndroidMemoryDumpSpec(String appId, String deviceId) {
        return new PerformanceTestSpec(INSPECTOR_ANDROID_MEMORY_DUMP, appId, deviceId, getNameByParam(INSPECTOR_ANDROID_MEMORY_DUMP, appId, deviceId));
    }

    public static PerformanceTestSpec createAndroidMemoryInfoSpec(String appId, String deviceId) {
        return new PerformanceTestSpec(INSPECTOR_ANDROID_MEMORY_INFO, appId, deviceId, getNameByParam(INSPECTOR_ANDROID_MEMORY_INFO, appId, deviceId));
    }

    public static PerformanceTestSpec createAndroidBatteryInfoSpec(String appId, String deviceId) {
        return new PerformanceTestSpec(INSPECTOR_ANDROID_BATTERY_INFO, appId, deviceId, getNameByParam(INSPECTOR_ANDROID_BATTERY_INFO, appId, deviceId));
    }

    public static PerformanceTestSpec createWindowsBatteryInfoSpec(String appId, String deviceId) {
        return new PerformanceTestSpec(INSPECTOR_WIN_BATTERY, appId, deviceId, getNameByParam(INSPECTOR_WIN_BATTERY, appId, deviceId));
    }

    public static PerformanceTestSpec createWindowsMemoryInfoSpec(String appId, String deviceId) {
        return new PerformanceTestSpec(INSPECTOR_WIN_MEMORY, appId, deviceId, getNameByParam(INSPECTOR_WIN_MEMORY, appId, deviceId));
    }

    public PerformanceTestSpec rename(String name) {
        this.name = name;
        return this;
    }

    private static String getNameByParam(String inspector, String appId, String deviceId) {
        return String.format("PerfTesting: get %s for %s on %s", appId, deviceId, inspector);
    }

}
