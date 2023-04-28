// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.management.device.impl;

import cn.hutool.core.img.ImgUtil;
import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.logger.impl.WindowsLogCollector;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.AppiumServerManager;
import com.microsoft.hydralab.common.management.device.DeviceType;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.screen.WindowsScreenRecorder;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import io.appium.java_client.windows.WindowsDriver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WindowsDeviceDriver extends AbstractDeviceDriver {

    static final Logger classLogger = LoggerFactory.getLogger(WindowsDeviceDriver.class);
    private static final int MAJOR_APPIUM_VERSION = 1;
    private static final int MINOR_APPIUM_VERSION = -1;

    public WindowsDeviceDriver(AgentManagementService agentManagementService,
                               AppiumServerManager appiumServerManager) {
        super(agentManagementService, appiumServerManager);
    }

    @Override
    public void init() {
        try {
            WindowsDriver windowsDriver = appiumServerManager.getWindowsRootDriver(classLogger);
            windowsDriver.getScreenshotAs(OutputType.FILE);
            InetAddress localHost = InetAddress.getLocalHost();
            DeviceInfo deviceInfo = new DeviceInfo();
            String uuid = UUID.randomUUID().toString();
            deviceInfo.setSerialNum(localHost.getHostName());
            deviceInfo.setDeviceId(uuid);
            deviceInfo.setName(localHost.getHostName());
            deviceInfo.setModel(System.getProperties().getProperty("os.name"));
            deviceInfo.setOsVersion(System.getProperties().getProperty("os.version"));
            deviceInfo.setOsSDKInt(System.getProperties().getProperty("os.arch"));
            deviceInfo.setScreenDensity(0);
            deviceInfo.setScreenSize("");
            deviceInfo.setType(DeviceType.WINDOWS.name());
            deviceInfo.setStatus(DeviceInfo.ONLINE);
            agentManagementService.getDeviceStatusListenerManager().onDeviceConnected(deviceInfo);
        } catch (Exception e) {
            throw new HydraLabRuntimeException(500, "WindowsDeviceDriver init failed", e);
        }
    }

    @Override
    public List<EnvCapabilityRequirement> getEnvCapabilityRequirements() {
        return List.of(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.appium, MAJOR_APPIUM_VERSION, MINOR_APPIUM_VERSION));
    }

    @Override
    public void wakeUpDevice(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) {

    }

    @Override
    public void backToHome(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) {

    }

    @Override
    public void grantPermission(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull String permissionName, @Nullable Logger logger) {

    }

    @Override
    public void addToBatteryWhiteList(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull Logger logger) {

    }

    @Override
    public boolean installApp(@NotNull DeviceInfo deviceInfo, @NotNull String packagePath, @Nullable Logger logger) {
        return false;
    }

    @Override
    public boolean uninstallApp(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger) {
        return false;
    }

    @Override
    public void resetPackage(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger) {

    }

    @Override
    public void pushFileToDevice(@NotNull DeviceInfo deviceInfo, @NotNull String pathOnAgent, @NotNull String pathOnDevice, @Nullable Logger logger) throws Exception {

    }

    @Override
    public void pullFileFromDevice(@NotNull DeviceInfo deviceInfo, @NotNull String pathOnDevice, @Nullable Logger logger) throws Exception {

    }

    @Override
    public LogCollector getLogCollector(@NotNull DeviceInfo deviceInfo, @NotNull String pkgName, @NotNull TestRun testRun, @NotNull Logger logger) {
        return new WindowsLogCollector(deviceInfo, pkgName, testRun, logger);
    }

    @Override
    public void setProperty(@NotNull DeviceInfo deviceInfo, @NotNull String property, String val, @Nullable Logger logger) {

    }

    @Override
    public boolean setDefaultLauncher(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull String defaultActivity, @Nullable Logger logger) {
        return false;
    }

    @Override
    public boolean isAppInstalled(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger) {
        return false;
    }

    @Override
    public boolean grantProjectionAndBatteryPermission(@NotNull DeviceInfo deviceInfo, @NotNull String recordPackageName, @Nullable Logger logger) {
        return false;
    }

    @Override
    public void testDeviceSetup(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) {

    }

    @Override
    public void removeFileInDevice(DeviceInfo deviceInfo, String pathOnDevice, Logger logger) {

    }

    @Override
    public void testDeviceUnset(DeviceInfo deviceInfo, Logger logger) {

    }

    @Override
    public WebDriver getAppiumDriver(DeviceInfo deviceInfo, Logger logger) {
        return appiumServerManager.getWindowsRootDriver(logger);
    }

    @Override
    public void quitAppiumDriver(DeviceInfo deviceInfo, Logger logger) {

    }

    @Override
    public void execCommandOnDevice(DeviceInfo deviceInfo, String command, Logger logger) {

    }

    @Override
    public void screenCapture(DeviceInfo deviceInfo, String outputFile, Logger logger) throws IOException {
        File scrFile = appiumServerManager.getWindowsRootDriver(logger).getScreenshotAs(OutputType.FILE);
        BufferedImage screenshot = ImageIO.read(scrFile);
        ImgUtil.scale(screenshot, new File(outputFile), 0.7f);
    }

    @Override
    public ScreenRecorder getScreenRecorder(DeviceInfo deviceInfo, File folder, Logger logger) {
        return new WindowsScreenRecorder(this, deviceInfo, folder, logger);
    }
}
