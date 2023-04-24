// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.management.device.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.logger.impl.IOSLogCollector;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.AppiumServerManager;
import com.microsoft.hydralab.common.management.device.DeviceType;
import com.microsoft.hydralab.common.screen.IOSAppiumScreenRecorderForMac;
import com.microsoft.hydralab.common.screen.IOSAppiumScreenRecorderForWindows;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.AgentConstant;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.IOSUtils;
import com.microsoft.hydralab.common.util.ShellUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.hydralab.common.util.AgentConstant.UNKNOWN_IOS_MODEL;

public class IOSDeviceDriver extends AbstractDeviceDriver {
    public static final String iOSDeviceManufacturer = "Apple";
    static final Logger classLogger = LoggerFactory.getLogger(IOSDeviceDriver.class);
    private final Map<String, DeviceInfo> iOSDeviceInfoMap = new HashMap<>();
    private static final int MAJOR_APPIUM_VERSION = 1;
    private static final int MINOR_APPIUM_VERSION = -1;
    private static final int MAJOR_TIDEVICE_VERSION = 0;
    private static final int MINOR_TIDEVICE_VERSION = 10;

    public IOSDeviceDriver(AgentManagementService agentManagementService,
                           AppiumServerManager appiumServerManager) {
        super(agentManagementService, appiumServerManager);
        if (ShellUtils.isConnectedToWindowsOS) {
            IOSAppiumScreenRecorderForWindows.copyScript(agentManagementService.getTestBaseDir());
        } else {
            // Mac, unix or linux
            // do nothing
        }
    }

    @Override
    public void init() {
        try {
            ShellUtils.killProcessByCommandStr("tidevice", classLogger);
            IOSUtils.startIOSDeviceWatcher(classLogger, this);
        } catch (Exception e) {
            throw new HydraLabRuntimeException(500, "IOSDeviceDriver init failed", e);
        }
    }

    @Override
    public List<EnvCapabilityRequirement> getEnvCapabilityRequirements() {
        // todo XCCode / iTunes
        List<EnvCapabilityRequirement> envCapabilityRequirements = new ArrayList<>();
        envCapabilityRequirements.add(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.appium, MAJOR_APPIUM_VERSION, MINOR_APPIUM_VERSION));
        envCapabilityRequirements.add(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.tidevice, MAJOR_TIDEVICE_VERSION, MINOR_TIDEVICE_VERSION));
        return envCapabilityRequirements;
    }

    @Override
    public void screenCapture(@NotNull DeviceInfo deviceInfo, @NotNull String path, @Nullable Logger logger) {
        IOSUtils.takeScreenshot(deviceInfo.getSerialNum(), path, classLogger);
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
    public void addToBatteryWhiteList(@NotNull DeviceInfo deviceInfo, @NotNull String packageName,
                                      @NotNull Logger logger) {
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
        // todo Nothing Implemented for iOS in resetPackage replace with uninstallApp
        uninstallApp(deviceInfo, packageName, logger);
        classLogger.warn("!!!  As there is no implementation for package reset in iOS, what we did here is uninstall the app, please be AWARE  !!!");
    }

    @Override
    public void pushFileToDevice(@NotNull DeviceInfo deviceInfo, @NotNull String pathOnAgent,
                                 @NotNull String pathOnDevice, @Nullable Logger logger) {
        classLogger.info("Nothing Implemented for iOS in " + currentMethodName());
    }

    @Override
    public void pullFileFromDevice(@NotNull DeviceInfo deviceInfo, @NotNull String pathOnDevice,
                                   @Nullable Logger logger) {
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
        return new IOSLogCollector(deviceInfo, pkgName, testRun, logger);
    }

    @Override
    public void setProperty(DeviceInfo deviceInfo, String property, String val, Logger logger) {
        classLogger.info("Nothing Implemented for iOS in " + currentMethodName());
    }

    @Override
    public boolean setDefaultLauncher(DeviceInfo deviceInfo, String packageName, String defaultActivity,
                                      Logger logger) {
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
                        agentManagementService.getDeviceStatusListenerManager().onDeviceConnected(info);
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
                    agentManagementService.getDeviceStatusListenerManager().onDeviceInactive(info);
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
                agentManagementService.getDeviceStatusListenerManager().onDeviceConnected(info);
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
        deviceInfo.setType(DeviceType.IOS.name());
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
    public boolean grantProjectionAndBatteryPermission(DeviceInfo deviceInfo, String recordPackageName,
                                                       Logger logger) {
        classLogger.info("Nothing Implemented for iOS in " + currentMethodName());
        return true;
    }

    @Override
    public void testDeviceSetup(DeviceInfo deviceInfo, Logger logger) {

    }

    @Override
    public void removeFileInDevice(DeviceInfo deviceInfo, String pathOnDevice, Logger logger) {

    }

    @Override
    public void testDeviceUnset(DeviceInfo deviceInfo, Logger logger) {

    }

    @Override
    public WebDriver getAppiumDriver(DeviceInfo deviceInfo, Logger logger) {
        return appiumServerManager.getIOSDriver(deviceInfo, logger);
    }

    @Override
    public void quitAppiumDriver(DeviceInfo deviceInfo, Logger logger) {
        appiumServerManager.quitIOSDriver(deviceInfo, logger);
    }

    @Override
    public void execCommandOnDevice(DeviceInfo deviceInfo, String command, Logger logger) {
        classLogger.info("Nothing Implemented for iOS in " + currentMethodName());
    }

    private String currentMethodName() {
        return Thread.currentThread().getStackTrace()[2].getMethodName();
    }
}