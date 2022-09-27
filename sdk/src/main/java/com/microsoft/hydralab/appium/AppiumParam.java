// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.appium;

public class AppiumParam {
    String deviceId = null;
    String deviceName = null;
    String osVersion = null;
    int wdaPort = 8100;
    String apkPath = null;
    String outputDir = null;

    public AppiumParam() {
    }

    @Override
    public String toString() {
        return "AppiumParam{" +
                "deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", osVersion='" + osVersion + '\'' +
                ", wdaPort=" + wdaPort +
                ", apkPath='" + apkPath + '\'' +
                ", outputDir='" + outputDir + '\'' +
                '}';
    }

    public AppiumParam(String deviceId, String deviceName, String osVersion, int wdaPort, String apkPath, String outputDir) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.osVersion = osVersion;
        this.wdaPort = wdaPort;
        this.apkPath = apkPath;
        this.outputDir = outputDir;
    }

    public String getDeviceId() {
        return deviceId;
    }
    public String getDeviceName() {
        return deviceName;
    }
    public String getDeviceOsVersion() {
        return osVersion;
    }
    public int getWdaPort() {
        return wdaPort;
    }
    public String getApkPath() {
        return apkPath;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public String getDeviceId(String defaultValue) {
        if (deviceId == null) {
            return defaultValue;
        }
        return deviceId;
    }

    public String getApkPath(String defaultValue) {
        if (apkPath == null) {
            return defaultValue;
        }
        return apkPath;
    }

    public String getOutputDir(String defaultValue) {
        if (outputDir == null) {
            return defaultValue;
        }
        return outputDir;
    }
}
