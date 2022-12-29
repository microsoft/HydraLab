package com.microsoft.hydralab.performance;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PerformanceTestSpec {
    public static final String INSPECTOR_ANDROID_MEMORY_DUMP = "AndroidMemoryDump";
    public static final String INSPECTOR_ANDROID_MEMORY_INFO = "AndroidMemoryInfo";
    public static final String INSPECTOR_ANDROID_BATTERY_INFO = "AndroidBatteryInfo";
    public static final String INSPECTOR_WIN_BATTERY = "WindowsBattery";
    public static final String INSPECTOR_WIN_MEMORY = "WindowsMemory";
    public String id = UUID.randomUUID().toString();
    String inspector;
    String appId;
    String deviceId;
    String name;

    File resultFolder;
    public long interval;

    public PerformanceTestSpec(String inspector, String appId, String deviceId, String name) {
        this.inspector = inspector;
        this.appId = appId;
        this.deviceId = deviceId;
        this.name = name;
    }

    public String getInspectors() {
        return inspector;
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
