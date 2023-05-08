// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.management.device.impl;

import com.microsoft.hydralab.common.entity.common.AgentUser;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.management.AppiumServerManager;
import com.microsoft.hydralab.common.management.device.DeviceDriver;
import com.microsoft.hydralab.common.management.device.DeviceType;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DeviceDriverManager implements DeviceDriver {
    static final Logger classLogger = LoggerFactory.getLogger(DeviceDriverManager.class);
    ConcurrentMap<DeviceType, DeviceDriver> deviceDriverMap = new ConcurrentHashMap<>();

    public void addDeviceDriver(DeviceType deviceType, DeviceDriver deviceDriver) {
        deviceDriverMap.put(deviceType, deviceDriver);
    }

    public void init() {
        for (DeviceType deviceType : deviceDriverMap.keySet()) {
            try {
                classLogger.info("Try to init device driver: {}", deviceType);
                deviceDriverMap.get(deviceType).init();
            } catch (Exception e) {
                classLogger.error("Init device driver failed: {}", deviceType, e);
                deviceDriverMap.remove(deviceType);
            }
        }
        if (deviceDriverMap.size() == 0) {
            System.exit(500);
        }
    }

    private DeviceDriver getDeviceDriver(String deviceType) {
        DeviceDriver deviceDriver = deviceDriverMap.get(DeviceType.valueOf(deviceType));
        if (deviceDriver == null) {
            throw new HydraLabRuntimeException("Device driver not found: " + deviceType);
        }
        return deviceDriver;
    }

    @Override
    public File getScreenShot(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) {
        return getDeviceDriver(deviceInfo.getType()).getScreenShot(deviceInfo, logger);
    }

    @Override
    public File getScreenShotWithStrategy(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger, @NotNull AgentUser.BatteryStrategy batteryStrategy) {
        return getDeviceDriver(deviceInfo.getType()).getScreenShotWithStrategy(deviceInfo, logger, batteryStrategy);
    }

    @Override
    public void wakeUpDevice(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) {
        getDeviceDriver(deviceInfo.getType()).wakeUpDevice(deviceInfo, logger);
    }

    @Override
    public void unlockDevice(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) {
        getDeviceDriver(deviceInfo.getType()).unlockDevice(deviceInfo, logger);
    }

    @Override
    public void backToHome(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) {
        getDeviceDriver(deviceInfo.getType()).backToHome(deviceInfo, logger);
    }

    @Override
    public void grantPermission(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull String permissionName, @Nullable Logger logger) {
        getDeviceDriver(deviceInfo.getType()).grantPermission(deviceInfo, packageName, permissionName, logger);
    }

    @Override
    public void addToBatteryWhiteList(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull Logger logger) {
        getDeviceDriver(deviceInfo.getType()).addToBatteryWhiteList(deviceInfo, packageName, logger);
    }

    @Override
    public boolean installApp(@NotNull DeviceInfo deviceInfo, @NotNull String packagePath, @Nullable Logger logger) {
        String suffix = packagePath.substring(packagePath.lastIndexOf(".") + 1);
        if (!DeviceType.valueOf(deviceInfo.getType()).getSupportedAppSuffix().contains(suffix)) {
            return true;
        }
        return getDeviceDriver(deviceInfo.getType()).installApp(deviceInfo, packagePath, logger);
    }

    @Override
    public boolean uninstallApp(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger) {
        return getDeviceDriver(deviceInfo.getType()).uninstallApp(deviceInfo, packageName, logger);
    }

    @Override
    public void resetPackage(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger) {
        getDeviceDriver(deviceInfo.getType()).resetPackage(deviceInfo, packageName, logger);
    }

    @Override
    public void pushFileToDevice(@NotNull DeviceInfo deviceInfo, @NotNull String pathOnAgent, @NotNull String pathOnDevice, @Nullable Logger logger) throws Exception {
        getDeviceDriver(deviceInfo.getType()).pushFileToDevice(deviceInfo, pathOnAgent, pathOnDevice, logger);
    }

    @Override
    public void pullFileFromDevice(@NotNull DeviceInfo deviceInfo, @NotNull String pathOnDevice, @Nullable Logger logger) throws Exception {
        getDeviceDriver(deviceInfo.getType()).pullFileFromDevice(deviceInfo, pathOnDevice, logger);
    }

    @Override
    public ScreenRecorder getScreenRecorder(@NotNull DeviceInfo deviceInfo, @NotNull File folder, @Nullable Logger logger) {
        return getDeviceDriver(deviceInfo.getType()).getScreenRecorder(deviceInfo, folder, logger);
    }

    @Override
    public boolean grantAllTaskNeededPermissions(@NotNull DeviceInfo deviceInfo, @NotNull TestTask task, @Nullable Logger logger) {
        return getDeviceDriver(deviceInfo.getType()).grantAllTaskNeededPermissions(deviceInfo, task, logger);
    }

    @Override
    public boolean grantAllPackageNeededPermissions(@NotNull DeviceInfo deviceInfo, @NotNull File packageFile, @NotNull String targetPackage, boolean allowCustomizedPermissions,
                                                    @Nullable Logger logger) {
        return getDeviceDriver(deviceInfo.getType()).grantAllPackageNeededPermissions(deviceInfo, packageFile, targetPackage, allowCustomizedPermissions, logger);
    }

    @Override
    public Logger getDeviceLogger(DeviceInfo device) {
        return getDeviceDriver(device.getType()).getDeviceLogger(device);
    }

    @Override
    public void updateAllDeviceInfo() {
        deviceDriverMap.values().forEach(DeviceDriver::updateAllDeviceInfo);
    }

    @Override
    public LogCollector getLogCollector(@NotNull DeviceInfo deviceInfo, @NotNull String pkgName, @NotNull TestRun testRun, @NotNull Logger logger) {
        return getDeviceDriver(deviceInfo.getType()).getLogCollector(deviceInfo, pkgName, testRun, logger);
    }

    @Override
    public void setProperty(@NotNull DeviceInfo deviceInfo, @NotNull String property, String val, @Nullable Logger logger) {
        getDeviceDriver(deviceInfo.getType()).setProperty(deviceInfo, property, val, logger);
    }

    @Override
    public boolean setDefaultLauncher(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull String defaultActivity, @Nullable Logger logger) {
        return getDeviceDriver(deviceInfo.getType()).setDefaultLauncher(deviceInfo, packageName, defaultActivity, logger);
    }

    @Override
    public boolean isAppInstalled(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger) {
        return getDeviceDriver(deviceInfo.getType()).isAppInstalled(deviceInfo, packageName, logger);
    }

    @Override
    public boolean grantProjectionAndBatteryPermission(@NotNull DeviceInfo deviceInfo, @NotNull String recordPackageName, @Nullable Logger logger) {
        return getDeviceDriver(deviceInfo.getType()).grantProjectionAndBatteryPermission(deviceInfo, recordPackageName, logger);
    }

    @Override
    public void testDeviceSetup(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) {
        getDeviceDriver(deviceInfo.getType()).testDeviceSetup(deviceInfo, logger);
    }

    @Override
    public void removeFileInDevice(DeviceInfo deviceInfo, String pathOnDevice, Logger logger) {
        getDeviceDriver(deviceInfo.getType()).removeFileInDevice(deviceInfo, pathOnDevice, logger);
    }

    @Override
    public void testDeviceUnset(DeviceInfo deviceInfo, Logger logger) {
        getDeviceDriver(deviceInfo.getType()).testDeviceUnset(deviceInfo, logger);
    }

    @Override
    public WebDriver getAppiumDriver(DeviceInfo deviceInfo, Logger logger) {
        return getDeviceDriver(deviceInfo.getType()).getAppiumDriver(deviceInfo, logger);
    }

    @Override
    public void quitAppiumDriver(DeviceInfo deviceInfo, Logger logger) {
        getDeviceDriver(deviceInfo.getType()).quitAppiumDriver(deviceInfo, logger);
    }

    @Override
    public void execCommandOnDevice(DeviceInfo deviceInfo, String command, Logger logger) {
        getDeviceDriver(deviceInfo.getType()).execCommandOnDevice(deviceInfo, command, logger);
    }

    @Override
    public void execCommandOnAgent(DeviceInfo deviceInfo, String command, Logger logger) {
        getDeviceDriver(deviceInfo.getType()).execCommandOnAgent(deviceInfo, command, logger);
    }

    @Override
    public AppiumServerManager getAppiumServerManager() {
        ArrayList<DeviceType> keys = new ArrayList<>(deviceDriverMap.keySet());
        return deviceDriverMap.get(keys.get(0)).getAppiumServerManager();
    }
}
