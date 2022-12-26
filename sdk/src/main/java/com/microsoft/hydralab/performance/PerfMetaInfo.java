package com.microsoft.hydralab.performance;

public class PerfMetaInfo {
    public static final int FLAG_MEM = 0x01;
    public static final int FLAG_BATTERY = 0x02;
    public static final int FLAG_LATENCY = 0x04;
    public static final int FLAG_CPU = 0x08;

    int typeFlag;
    String appId;
    String deviceId;
    String name;
    String metricsDir;

    public PerfMetaInfo(int typeFlag, String appId, String deviceId, String name) {
        this.typeFlag = typeFlag;
        this.appId = appId;
        this.deviceId = deviceId;
        this.name = name;
    }

    public int getTypeFlag() {
        return typeFlag;
    }

    public void setTypeFlag(int typeFlag) {
        this.typeFlag = typeFlag;
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

    public String getMetricsDir() {
        return metricsDir;
    }

    public void setMetricsDir(String metricsDir) {
        this.metricsDir = metricsDir;
    }

    @Override
    public String toString() {
        return "PerfMetaInfo{" +
                "typeFlag='" + typeFlag + '\'' +
                ", appId='" + appId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", stepName='" + name + '\'' +
                '}';
    }
}
