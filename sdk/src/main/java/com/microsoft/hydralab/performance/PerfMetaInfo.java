package com.microsoft.hydralab.performance;

public class PerfMetaInfo {
    public static final String TYPE_MEM = "TYPE_MEM";
    public static final String TYPE_BATTERY = "TYPE_BATTERY";
    public static final String TYPE_LATENCY = "TYPE_LATENCY";
    public static final String TYPE_CPU = "TYPE_CPU";

    String type;
    String appId;
    String deviceId;
    String stepName;

    public PerfMetaInfo(String type, String appId, String deviceId, String stepName) {
        this.type = type;
        this.appId = appId;
        this.deviceId = deviceId;
        this.stepName = stepName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    @Override
    public String toString() {
        return "PerfMetaInfo{" +
                "type='" + type + '\'' +
                ", appId='" + appId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", stepName='" + stepName + '\'' +
                '}';
    }
}
