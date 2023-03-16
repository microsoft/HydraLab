// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.management.device.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.logger.impl.IOSLogCollector;
import com.microsoft.hydralab.common.management.device.DeviceType;
import com.microsoft.hydralab.common.management.device.TestDeviceManager;
import com.microsoft.hydralab.common.screen.IOSAppiumScreenRecorderForMac;
import com.microsoft.hydralab.common.screen.IOSAppiumScreenRecorderForWindows;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.AgentConstant;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.IOSUtils;
import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.t2c.runner.*;
import com.microsoft.hydralab.t2c.runner.controller.BaseDriverController;
import com.microsoft.hydralab.t2c.runner.controller.IOSDriverController;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.hydralab.common.util.AgentConstant.UNKNOWN_IOS_MODEL;

public class IOSTestDeviceManager extends TestDeviceManager {
    public static final String iOSDeviceManufacturer = "Apple";
    static final Logger classLogger = LoggerFactory.getLogger(IOSTestDeviceManager.class);
    private final Map<String, DeviceInfo> iOSDeviceInfoMap = new HashMap<>();

    private boolean isConnectedToWindowsOS;

    public boolean isDeviceConnectedToWindows() {
        return isConnectedToWindowsOS;
    }

    @Override
    public void init() {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            isConnectedToWindowsOS = true;
            IOSAppiumScreenRecorderForWindows.copyScript(agentManagementService.getTestBaseDir());
        } else {
            // Mac, unix or linux
            isConnectedToWindowsOS = false;
        }
        try {
            ShellUtils.killProcessByCommandStr("tidevice", classLogger);
            IOSUtils.startIOSDeviceWatcher(classLogger, this);
        } catch (Exception e) {
            throw new HydraLabRuntimeException(500, "IOSDeviceManager init failed", e);
        }
    }

    @Override
    public File getScreenShot(DeviceInfo deviceInfo, Logger logger) throws Exception {
        File screenshotImageFile = deviceInfo.getScreenshotImageFile();
        if (screenshotImageFile == null) {
            screenshotImageFile = new File(agentManagementService.getScreenshotDir(),
                    deviceInfo.getName().replace(" ", "") + "-" + deviceInfo.getSerialNum() + ".jpg");
            deviceInfo.setScreenshotImageFile(screenshotImageFile);
            String imageRelPath = screenshotImageFile.getAbsolutePath()
                    .replace(new File(agentManagementService.getDeviceStoragePath()).getAbsolutePath(), "");
            imageRelPath =
                    agentManagementService.getDeviceFolderUrlPrefix() + imageRelPath.replace(File.separator, "/");
            deviceInfo.setImageRelPath(imageRelPath);
        }
        IOSUtils.takeScreenshot(deviceInfo.getSerialNum(), screenshotImageFile.getAbsolutePath(), classLogger);
        deviceInfo.setScreenshotUpdateTimeMilli(System.currentTimeMillis());
        StorageFileInfo fileInfo =
                new StorageFileInfo(screenshotImageFile, "device/screenshots/" + screenshotImageFile.getName(),
                        StorageFileInfo.FileType.SCREENSHOT, EntityType.SCREENSHOT);
        String fileDownloadUrl =
                agentManagementService.getStorageServiceClientProxy().upload(screenshotImageFile, fileInfo)
                        .getBlobUrl();
        if (StringUtils.isBlank(fileDownloadUrl)) {
            classLogger.warn("Screenshot download url is empty for device {}", deviceInfo.getName());
        } else {
            deviceInfo.setScreenshotImageUrl(fileDownloadUrl);
        }
        return screenshotImageFile;
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
        classLogger.info("Nothing Implemented for iOS in " + currentMethodName());
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
        DeviceInfo deviceInfo = new DeviceInfo(this);
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

    @Override
    public boolean runAppiumT2CTest(DeviceInfo deviceInfo, File jsonFile, Logger reportLogger) {
        reportLogger.info("Start T2C Test");
        T2CJsonParser t2CJsonParser = new T2CJsonParser(reportLogger);
        TestInfo testInfo = t2CJsonParser.parseJsonFile(jsonFile.getAbsolutePath());
        Assert.notNull(testInfo, "Failed to parse the json file for test automation.");

        String testWindowsApp = "";
        Map<String, BaseDriverController> driverControllerMap = new HashMap<>();

        // Check device requirements
        int iosCount = 0;

        for (DriverInfo driverInfo : testInfo.getDrivers()) {
            if (driverInfo.getPlatform().equalsIgnoreCase("android")) {
                throw new RuntimeException("No Android device connected to this agent");
            }
            if (driverInfo.getPlatform().equalsIgnoreCase("browser")) {
                throw new RuntimeException("No Browser supported in this agent");
            }
            if (driverInfo.getPlatform().equalsIgnoreCase("ios")) {
                iosCount++;
            }
        }
        // TODO: upgrade to check the available device count on the agent
        Assert.isTrue(iosCount <= 1, "Only support one iOS device for now.");

        try {
            // Prepare drivers
            for (DriverInfo driverInfo : testInfo.getDrivers()) {
                if (driverInfo.getPlatform().equalsIgnoreCase("iOS")) {
                    IOSDriverController iosDriverController = new IOSDriverController(
                            appiumServerManager.getIOSDriver(deviceInfo, reportLogger),
                            deviceInfo.getSerialNum(), reportLogger);
                    driverControllerMap.put(driverInfo.getId(), iosDriverController);
                    if (!StringUtils.isEmpty(driverInfo.getLauncherApp())) {
                        iosDriverController.activateApp(driverInfo.getLauncherApp());
                    }
                    reportLogger.info("Successfully init an iOS driver: " + deviceInfo.getSerialNum());
                }
            }

            ArrayList<ActionInfo> caseList = testInfo.getActions();

            for (ActionInfo actionInfo : caseList) {
                BaseDriverController driverController = driverControllerMap.get(actionInfo.getDriverId());
                reportLogger.info("Start step: " + actionInfo.getId() + ", description: " + actionInfo.getDescription() + "action: " + actionInfo.getActionType() + " on element: "
                        + (actionInfo.getTestElement() != null ? actionInfo.getTestElement().getElementInfo() : "No Element"));
                T2CAppiumUtils.doAction(driverController, actionInfo, reportLogger);
            }
        } catch (Exception e) {
            reportLogger.error("T2C Test Error: ", e);
            throw e;
        } finally {
            appiumServerManager.quitIOSDriver(deviceInfo, reportLogger);
            reportLogger.info("Finish T2C Test");
        }

        return true;
    }
}