// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.management.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.logger.impl.IOSLogCollector;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.screen.IOSAppiumScreenRecorderForMac;
import com.microsoft.hydralab.common.screen.IOSAppiumScreenRecorderForWindows;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.AgentConstant;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.IOSUtils;
import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.common.util.blob.DeviceNetworkBlobConstants;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.microsoft.hydralab.common.util.AgentConstant.UNKNOWN_IOS_MODEL;

public class IOSDeviceManager extends DeviceManager {
    public static final String iOSDeviceManufacturer = "Apple";
    static final Logger classLogger = LoggerFactory.getLogger(IOSDeviceManager.class);
    private final Map<String, DeviceInfo> iOSDeviceInfoMap = new HashMap<>();

    private boolean isConnectedToWindowsOS;

    public boolean isDeviceConnectedToWindows() {
        return isConnectedToWindowsOS;
    }

    @Override
    public void init() throws Exception {
        String osName = System.getProperty("os.name");
        classLogger.info("Devices are connected to " + osName);
        if (osName.startsWith("Windows")) {
            isConnectedToWindowsOS = true;
            IOSAppiumScreenRecorderForWindows.copyScript(testBaseDir);
        } else {
            // Mac, unix or linux
            isConnectedToWindowsOS = false;
        }
        ShellUtils.killProcessByCommandStr("tidevice", classLogger);
        IOSUtils.startIOSDeviceWatcher(classLogger, this);
    }

    @Override
    public Set<DeviceInfo> getDeviceList(Logger logger) {
        Set<DeviceInfo> set = new HashSet<>();
        Set<Map.Entry<String, DeviceInfo>> entries = null;
        synchronized (this) {
            entries = new HashSet<>(iOSDeviceInfoMap.entrySet());
        }
        for (Map.Entry<String, DeviceInfo> entry : entries) {
            DeviceInfo value = entry.getValue();
            if (value != null) {
                set.add(value);
            }
        }
        return set;
    }

    @Override
    public Set<DeviceInfo> getActiveDeviceList(Logger logger) {
        Set<DeviceInfo> set = new HashSet<>();
        Set<Map.Entry<String, DeviceInfo>> entries = null;
        synchronized (this) {
            entries = new HashSet<>(iOSDeviceInfoMap.entrySet());
        }
        for (Map.Entry<String, DeviceInfo> entry : entries) {
            DeviceInfo value = entry.getValue();
            if (value == null || !value.isAlive()) {
                classLogger.debug("Invalid device: {}", value);
                continue;
            }
            set.add(value);
        }
        return set;
    }

    @Override
    public File getScreenShot(DeviceInfo deviceInfo, Logger logger) throws Exception {
        File screenshotImageFile = deviceInfo.getScreenshotImageFile();
        if (screenshotImageFile == null) {
            screenshotImageFile = new File(getScreenshotDir(), deviceInfo.getName().replace(" ", "") + "-" + deviceInfo.getSerialNum() + ".jpg");
            deviceInfo.setScreenshotImageFile(screenshotImageFile);
            String imageRelPath = screenshotImageFile.getAbsolutePath().replace(new File(getDeviceStoragePath()).getAbsolutePath(), "");
            imageRelPath = getDeviceFolderUrlPrefix() + imageRelPath.replace(File.separator, "/");
            deviceInfo.setImageRelPath(imageRelPath);
        }
        IOSUtils.takeScreenshot(deviceInfo.getSerialNum(), screenshotImageFile.getAbsolutePath(), classLogger);
        deviceInfo.setScreenshotUpdateTimeMilli(System.currentTimeMillis());
        String blobUrl = blobStorageClient.uploadBlobFromFile(screenshotImageFile, DeviceNetworkBlobConstants.IMAGES_BLOB_NAME, "device/screenshots/" + screenshotImageFile.getName(), null);
        if (StringUtils.isBlank(blobUrl)) {
            classLogger.warn("blobUrl is empty for device {}", deviceInfo.getName());
        } else {
            deviceInfo.setScreenshotImageUrl(blobUrl);
        }
        return screenshotImageFile;
    }

    @Override
    public void resetDeviceByTestId(String testId, Logger logger) {
        Set<DeviceInfo> devices = getActiveDeviceList(logger).stream()
                .filter(adbDeviceInfo -> testId.equals(adbDeviceInfo.getRunningTaskId()))
                .collect(Collectors.toSet());
        for (DeviceInfo device : devices) {
            resetDevice(device.getSerialNum());
        }
    }

    private void resetDevice(String serialNum) {
        DeviceInfo deviceInfo = iOSDeviceInfoMap.get(serialNum);
        if (deviceInfo == null) {
            return;
        }
        synchronized (deviceInfo) {
            deviceInfo.reset();
        }
    }

    @Override
    public void wakeUpDevice(DeviceInfo deviceInfo, Logger logger) {
        classLogger.info("Unlocking may not work as expected, please keep your device wake.");
        getAppiumServerManager().getIOSDriver(deviceInfo, logger).unlockDevice();
    }

    @Override
    public void backToHome(DeviceInfo deviceInfo, Logger logger) {
//        classLogger.info("Nothing Implemented for iOS in " + currentMethodName());
        getAppiumServerManager().getIOSDriver(deviceInfo, logger).runAppInBackground(Duration.ofSeconds(-1));
    }

    @Override
    public void grantPermission(DeviceInfo deviceInfo, String packageName, String permissionName, Logger logger) {
        classLogger.info("Nothing Implemented for iOS in " + currentMethodName());
    }

    @Override
    public void addToBatteryWhiteList(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull Logger logger) {
        classLogger.info("Nothing Implemented for iOS in " + currentMethodName());
    }

    @Override
    public boolean installApp(DeviceInfo deviceInfo, String packagePath, Logger logger) {
        IOSUtils.installApp(deviceInfo.getSerialNum(), packagePath, logger);
        return true;
    }

    @Override
    public boolean uninstallApp(DeviceInfo deviceInfo, String packageName, Logger logger) {
        String result = IOSUtils.uninstallApp(deviceInfo.getSerialNum(), packageName, logger);
        return result != null && !isAppInstalled(deviceInfo, packageName, logger);
    }

    @Override
    public void resetPackage(DeviceInfo deviceInfo, String packageName, Logger logger) {
        classLogger.info("Nothing Implemented for iOS in " + currentMethodName());
    }

    @Override
    public ScreenRecorder getScreenRecorder(DeviceInfo deviceInfo, File folder, Logger logger) {
        if (ShellUtils.isConnectedToWindowsOS) {
            return new IOSAppiumScreenRecorderForWindows(this, deviceInfo, folder.getAbsolutePath());
        } else {
            return new IOSAppiumScreenRecorderForMac(this, deviceInfo, folder.getAbsolutePath());
        }
    }

    @Override
    public LogCollector getLogCollector(DeviceInfo deviceInfo, String pkgName, TestRun testRun, Logger logger) {
        return new IOSLogCollector(this, deviceInfo, pkgName, testRun, logger);
    }

    @Override
    public void setProperty(DeviceInfo deviceInfo, String property, String val, Logger logger) {
        classLogger.info("Nothing Implemented for iOS in " + currentMethodName());
    }

    @Override
    public void updateIsPrivateByDeviceSerial(String deviceSerial, Boolean isPrivate) {
        DeviceInfo deviceInfo;
        synchronized (iOSDeviceInfoMap) {
            deviceInfo = iOSDeviceInfoMap.get(deviceSerial);
        }
        if (deviceInfo != null) {
            deviceInfo.setIsPrivate(isPrivate);
        }
    }

    @Override
    public boolean setDefaultLauncher(DeviceInfo deviceInfo, String packageName, String defaultActivity, Logger logger) {
        classLogger.info("Nothing Implemented for iOS in " + currentMethodName());
        return true;
    }

    @Override
    public boolean isAppInstalled(DeviceInfo deviceInfo, String packageName, Logger logger) {
        String result = IOSUtils.getAppList(deviceInfo.getSerialNum(), logger);
        if (result != null && !result.equals("")) {
            for (String line : result.split("\n")) {
                if (line.startsWith(packageName + " ")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void updateAllDeviceInfo() {
        String deviceListJsonStr = IOSUtils.getIOSDeviceListJsonStr(classLogger);
        JSONArray deviceListJson = JSON.parseArray(deviceListJsonStr);
        Map<String, DeviceInfo> latestDeviceInfoMap = new HashMap<>();

        if (deviceListJson != null) {
            for (int i = 0; i < deviceListJson.size(); i++) {
                JSONObject deviceObject = deviceListJson.getJSONObject(i);
                DeviceInfo info = parseJsonToDevice(deviceObject);
                latestDeviceInfoMap.put(info.getSerialNum(), info);
            }
        }
        synchronized (this) {
            for (Map.Entry<String, DeviceInfo> infoEntry : iOSDeviceInfoMap.entrySet()) {
                String serialNum = infoEntry.getKey();
                DeviceInfo info = infoEntry.getValue();
                DeviceInfo removedInfo = latestDeviceInfoMap.remove(serialNum);
                if (removedInfo != null) {
                    if (DeviceInfo.OFFLINE.equals(info.getStatus())) {
                        deviceStabilityMonitor.stabilityCheck(info, MobileDeviceState.ONLINE, Const.DeviceStability.BEHAVIOUR_GO_ONLINE);
                        info.setStatus(DeviceInfo.ONLINE);
//                        classLogger.info("Device " + serialNum + " updated");
                    }
                    info.setName(removedInfo.getName());
                    info.setOsVersion(removedInfo.getOsVersion());
                    info.setAbiList(removedInfo.getAbiList());

                } else {
                    // Device was disconnected
//                    classLogger.info("Device " + serialNum + " disconnected");
                    info.setStatus(DeviceInfo.OFFLINE);
                    deviceStabilityMonitor.stabilityCheck(info, MobileDeviceState.OFFLINE, Const.DeviceStability.BEHAVIOUR_DISCONNECT);
                    getAppiumServerManager().quitIOSDriver(info, classLogger);
                }
            }
            for (Map.Entry<String, DeviceInfo> infoEntry : latestDeviceInfoMap.entrySet()) {
                String serialNum = infoEntry.getKey();
                DeviceInfo info = infoEntry.getValue();
//                classLogger.info("Device " + serialNum + " connected");
                info.setStatus(DeviceInfo.ONLINE);
                // Add new connected devices
                iOSDeviceInfoMap.put(serialNum, info);
                deviceStabilityMonitor.stabilityCheck(info, MobileDeviceState.ONLINE, Const.DeviceStability.BEHAVIOUR_CONNECT);
            }
        }
    }

    public DeviceInfo parseJsonToDevice(JSONObject deviceObject) {
        DeviceInfo deviceInfo = new DeviceInfo();
        String udid = deviceObject.getString("udid");
        deviceInfo.setSerialNum(udid);
        deviceInfo.setDeviceId(udid);
        deviceInfo.setName(deviceObject.getString("name"));
        deviceInfo.setModel(deviceObject.getString("market_name"));
        deviceInfo.setOsVersion(deviceObject.getString("product_version"));
        deviceInfo.setBrand(iOSDeviceManufacturer);
        deviceInfo.setManufacturer(iOSDeviceManufacturer);
        deviceInfo.setOsSDKInt("");
        deviceInfo.setScreenDensity(0);
        deviceInfo.setScreenSize("");
        updateDeviceDetailByUdid(deviceInfo, udid);
        return deviceInfo;
    }

    public void updateDeviceDetailByUdid(DeviceInfo deviceInfo, String udid) {
        String deviceDetailJsonStr = IOSUtils.getIOSDeviceDetailInfo(udid, classLogger);
        JSONObject deviceDetailJson = JSON.parseObject(deviceDetailJsonStr);
        deviceInfo.setAbiList(deviceDetailJson.getString("CPUArchitecture"));

        String productType = deviceDetailJson.getString("ProductType");
        if ("-".equals(deviceInfo.getModel())) {
            String newName = "";
            if (!StringUtils.isEmpty(productType)) {
                newName = AgentConstant.iOSProductModelMap.get(productType);
            }

            if (!StringUtils.isEmpty(newName)) {
                deviceInfo.setModel(newName);
            } else {
                deviceInfo.setModel(UNKNOWN_IOS_MODEL);
            }
        }
    }

    @Override
    public boolean grantProjectionAndBatteryPermission(DeviceInfo deviceInfo, String recordPackageName, Logger logger) {
        classLogger.info("Nothing Implemented for iOS in " + currentMethodName());
        return true;
    }

    @Override
    public void testDeviceSetup(DeviceInfo deviceInfo, Logger logger) throws IOException {
        IOSUtils.proxyWDA(deviceInfo, logger);
    }

    @Override
    public void removeFileInDevice(DeviceInfo deviceInfo, String pathOnDevice, Logger logger) {

    }

    @Override
    public void testDeviceUnset(DeviceInfo deviceInfo, Logger logger) {
        IOSUtils.killProxyWDA(deviceInfo, logger);
    }

    @Override
    public WebDriver getMobileAppiumDriver(DeviceInfo deviceInfo, Logger logger) {
        return appiumServerManager.getIOSDriver(deviceInfo, logger);
    }

    @Override
    public void quitMobileAppiumDriver(DeviceInfo deviceInfo, Logger logger) {
        appiumServerManager.quitIOSDriver(deviceInfo, logger);
    }

    @Override
    public void execCommandOnDevice(DeviceInfo deviceInfo, String command, Logger logger) {
        classLogger.info("Nothing Implemented for iOS in " + currentMethodName());
    }

    private String currentMethodName() {
        return Thread.currentThread().getStackTrace()[2].getMethodName();
    }

    @Override
    public void runAppiumMonkey(DeviceInfo deviceInfo, String packageName, int round, Logger logger) {
        IOSUtils.stopApp(deviceInfo.getSerialNum(), packageName, logger);
        IOSUtils.launchApp(deviceInfo.getSerialNum(), packageName, logger);
        super.runAppiumMonkey(deviceInfo, packageName, round, logger);
    }
}
