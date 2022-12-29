package com.microsoft.hydralab.performance;

import java.util.List;

public class PerformanceTestSpec {
    public static final String INSPECTOR_ANDROID_MEMORY_DUMP = "AndroidMemoryDump";
    public static final String INSPECTOR_ANDROID_MEMORY_INFO = "AndroidMemoryInfo";
    public static final String INSPECTOR_ANDROID_BATTERY_INFO = "AndroidBatteryInfo";
    public static final String INSPECTOR_WIN_BATTERY = "WindowsBattery";
    public static final String INSPECTOR_WIN_MEMORY = "WindowsMemory";

    List<String> appliedInspectors;
    String appId;
    String deviceId;
    String name;

    public PerformanceTestSpec(List<String> appliedInspectors, String appId, String deviceId, String name) {
        this.appliedInspectors = appliedInspectors;
        this.appId = appId;
        this.deviceId = deviceId;
        this.name = name;
    }

    public List<String> getAppliedInspectors() {
        return appliedInspectors;
    }

    public void setAppliedInspectors(List<String> appliedInspectors) {
        this.appliedInspectors = appliedInspectors;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
