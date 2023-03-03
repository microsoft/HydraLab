// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.management.device.impl;

import cn.hutool.core.img.ImgUtil;
import com.android.ddmlib.TimeoutException;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.android.ddmlib.InstallException;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.screen.AppiumE2ETestRecorder;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.AppiumServerManager;
import com.microsoft.hydralab.common.management.device.TestDeviceManager;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.screen.WindowsScreenRecorder;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.ThreadUtils;
import com.microsoft.hydralab.common.util.blob.DeviceNetworkBlobConstants;
import com.microsoft.hydralab.t2c.runner.ActionInfo;
import com.microsoft.hydralab.t2c.runner.DriverInfo;
import com.microsoft.hydralab.t2c.runner.T2CAppiumUtils;
import com.microsoft.hydralab.t2c.runner.T2CJsonParser;
import com.microsoft.hydralab.t2c.runner.TestInfo;
import com.microsoft.hydralab.t2c.runner.controller.AndroidDriverController;
import com.microsoft.hydralab.t2c.runner.controller.BaseDriverController;
import com.microsoft.hydralab.t2c.runner.controller.EdgeDriverController;
import com.microsoft.hydralab.t2c.runner.controller.WindowsDriverController;
import io.appium.java_client.windows.WindowsDriver;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WindowsTestDeviceManager extends TestDeviceManager {
    static final Logger classLogger = LoggerFactory.getLogger(WindowsTestDeviceManager.class);

    public WindowsTestDeviceManager(AgentManagementService agentManagementService, AppiumServerManager appiumServerManager) {
        super(agentManagementService, appiumServerManager);
    }

    @Override
    public void init() {
        try {
            WindowsDriver windowsDriver = appiumServerManager.getWindowsRootDriver(classLogger);
            windowsDriver.getScreenshotAs(OutputType.FILE);
            InetAddress localHost = InetAddress.getLocalHost();
            DeviceInfo deviceInfo = new DeviceInfo(this);
            String udid = UUID.randomUUID().toString();
            deviceInfo.setSerialNum(localHost.getHostName());
            deviceInfo.setDeviceId(udid);
            deviceInfo.setName(localHost.getHostName());
            deviceInfo.setModel(System.getProperties().getProperty("os.name"));
            deviceInfo.setOsVersion(System.getProperties().getProperty("os.version"));
            deviceInfo.setOsSDKInt(System.getProperties().getProperty("os.arch"));
            deviceInfo.setScreenDensity(0);
            deviceInfo.setScreenSize("");
            deviceInfo.setType(DeviceInfo.DeviceType.WINDOWS.name());
            deviceInfo.setStatus(DeviceInfo.ONLINE);
            agentManagementService.getDeviceStatusListenerManager().onDeviceConnected(deviceInfo);
        } catch (Exception e) {
            classLogger.error("WindowsDeviceManager init failed", e);
            throw new HydraLabRuntimeException(500, "WindowsDeviceManager init failed", e);
        }
    }

    @Override
    public File getScreenShot(DeviceInfo deviceInfo, Logger logger) throws Exception {
        File screenShotImageFile = deviceInfo.getScreenshotImageFile();
        if (screenShotImageFile == null) {
            screenShotImageFile = new File(agentManagementService.getScreenshotDir(), deviceInfo.getName() + "-" + deviceInfo.getSerialNum() + "-" + "pc" + ".jpg");
            deviceInfo.setScreenshotImageFile(screenShotImageFile);
            String imageRelPath = screenShotImageFile.getAbsolutePath().replace(new File(agentManagementService.getDeviceStoragePath()).getAbsolutePath(), "");
            imageRelPath = agentManagementService.getDeviceFolderUrlPrefix() + imageRelPath.replace(File.separator, "/");
            deviceInfo.setImageRelPath(imageRelPath);
        }
        try {
            screenCapture(screenShotImageFile.getAbsolutePath());
        } catch (IOException e) {
            classLogger.error("Screen capture failed for device: {}", deviceInfo, e);
        }
        StorageFileInfo fileInfo = new StorageFileInfo(pcScreenShotImageFile, "device/screenshots/" + pcScreenShotImageFile.getName(), StorageFileInfo.FileType.SCREENSHOT, EntityType.SCREENSHOT);
        String fileDownloadUrl = agentManagementService.getStorageServiceClientProxy.upload(pcScreenShotImageFile, fileInfo).getBlobUrl();
        if (StringUtils.isBlank(fileDownloadUrl)) {
            classLogger.warn("Screenshot download url is empty for device {}", deviceInfo.getName());
        } else {
            deviceInfo.setScreenshotImageUrl(fileDownloadUrl);
        }
        return screenShotImageFile;
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
    public boolean installApp(@NotNull DeviceInfo deviceInfo, @NotNull String packagePath, @Nullable Logger logger) throws InstallException {
        return false;
    }

    @Override
    public boolean uninstallApp(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger) throws InstallException {
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
        return null;
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
    public void testDeviceSetup(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) throws IOException {

    }

    @Override
    public void removeFileInDevice(DeviceInfo deviceInfo, String pathOnDevice, Logger logger) {

    }

    @Override
    public void testDeviceUnset(DeviceInfo deviceInfo, Logger logger) {

    }

    @Override
    public WebDriver getMobileAppiumDriver(DeviceInfo deviceInfo, Logger logger) {
        return null;
    }

    @Override
    public void quitMobileAppiumDriver(DeviceInfo deviceInfo, Logger logger) {

    }

    @Override
    public void execCommandOnDevice(DeviceInfo deviceInfo, String command, Logger logger) {

    }

    public void screenCapture(String outputFile) throws IOException {
        File scrFile = appiumServerManager.getWindowsRootDriver(classLogger).getScreenshotAs(OutputType.FILE);
        BufferedImage screenshot = ImageIO.read(scrFile);
        ImgUtil.scale(screenshot, new File(outputFile), 0.7f);
    }

    @Override
    public ScreenRecorder getScreenRecorder(DeviceInfo deviceInfo, File folder, Logger logger) {
        return new WindowsScreenRecorder(this, deviceInfo, folder, logger);
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
        int androidCount = 0, edgeCount = 0;

        for (DriverInfo driverInfo : testInfo.getDrivers()) {
            if (driverInfo.getPlatform().equalsIgnoreCase("android")) {
                androidCount++;
            }
            if (driverInfo.getPlatform().equalsIgnoreCase("browser")) {
                edgeCount++;
            }
            if (driverInfo.getPlatform().equalsIgnoreCase("ios")) {
                throw new RuntimeException("No iOS device connected to this agent");
            }
        }
        // TODO: upgrade to check the available device count on the agent
        Assert.isTrue(androidCount <= 1, "No enough Android device to run this test.");
        Assert.isTrue(edgeCount <= 1, "No enough Edge browser to run this test.");

        try {
            // Prepare drivers
            for (DriverInfo driverInfo : testInfo.getDrivers()) {
                if (driverInfo.getPlatform().equalsIgnoreCase("android")) {
                    AndroidDriverController androidDriverController = new AndroidDriverController(
                            appiumServerManager.getAndroidDriver(deviceInfo, reportLogger), reportLogger);
                    driverControllerMap.put(driverInfo.getId(), androidDriverController);
                    if (!StringUtils.isEmpty(driverInfo.getLauncherApp())) {
                        androidDriverController.activateApp(driverInfo.getLauncherApp());
                    }
                    reportLogger.info("Successfully init an Android driver: " + deviceInfo.getSerialNum());
                }
                if (driverInfo.getPlatform().equalsIgnoreCase("windows")) {
                    WindowsDriver windowsDriver;
                    testWindowsApp = driverInfo.getLauncherApp();
                    if (testWindowsApp.length() > 0 && !testWindowsApp.equalsIgnoreCase("root")) {
                        windowsDriver = appiumServerManager.getWindowsAppDriver(testWindowsApp, reportLogger);
                    } else {
                        testWindowsApp = "Root";
                        windowsDriver = appiumServerManager.getWindowsRootDriver(reportLogger);
                    }
                    driverControllerMap.put(driverInfo.getId(),
                            new WindowsDriverController(windowsDriver, reportLogger));

                    reportLogger.info("Successfully init a Windows driver: " + testWindowsApp);
                }
                if (driverInfo.getPlatform().equalsIgnoreCase("browser")) {
                    appiumServerManager.getEdgeDriver(reportLogger);
                    if (!StringUtils.isEmpty(driverInfo.getInitURL())) {
                        appiumServerManager.getEdgeDriver(reportLogger).get(driverInfo.getInitURL());
                    }
                    // Waiting for loading url
                    ThreadUtils.safeSleep(5000);
                    driverControllerMap.put(driverInfo.getId(), new EdgeDriverController(
                            appiumServerManager.getWindowsEdgeDriver(reportLogger),
                            appiumServerManager.getEdgeDriver(reportLogger),
                            reportLogger));
                    reportLogger.info("Successfully init a Edge driver");
                }
            }

            ArrayList<ActionInfo> caseList = testInfo.getCases();

            for (ActionInfo actionInfo : caseList) {
                BaseDriverController driverController = driverControllerMap.get(actionInfo.getDriverId());
                T2CAppiumUtils.doAction(driverController, actionInfo, reportLogger);
                reportLogger.info("Do action: " + actionInfo.getActionType() + " on element: " + (actionInfo.getTestElement() != null ? actionInfo.getTestElement().getElementInfo() : "No Element"));
            }
        } catch (Exception e) {
            reportLogger.error("T2C Test Error: ", e);
            throw e;
        } finally {
            appiumServerManager.quitAndroidDriver(deviceInfo, reportLogger);
            if (testWindowsApp.length() > 0) {
                appiumServerManager.quitWindowsAppDriver(testWindowsApp, reportLogger);
            }
            appiumServerManager.quitEdgeDriver(reportLogger);
            appiumServerManager.quitWindowsEdgeDriver(reportLogger);
            reportLogger.info("Finish T2C Test");
        }

        return true;
    }
}
