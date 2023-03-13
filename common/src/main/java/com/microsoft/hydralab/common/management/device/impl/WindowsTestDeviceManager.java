// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.management.device.impl;

import cn.hutool.core.img.ImgUtil;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.screen.AppiumE2ETestRecorder;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.ThreadUtils;
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
import org.openqa.selenium.OutputType;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WindowsTestDeviceManager extends AndroidTestDeviceManager {

    @Override
    public File getScreenShot(DeviceInfo deviceInfo, Logger logger) throws Exception {
        File deviceFile = super.getScreenShot(deviceInfo, logger);
        File pcScreenShotImageFile = deviceInfo.getPcScreenshotImageFile();
        if (pcScreenShotImageFile == null) {
            pcScreenShotImageFile = new File(agentManagementService.getScreenshotDir(),
                    deviceInfo.getName() + "-" + deviceInfo.getSerialNum() + "-" + "pc" + ".jpg");
            deviceInfo.setPcScreenshotImageFile(pcScreenShotImageFile);
            String pcImageRelPath = pcScreenShotImageFile.getAbsolutePath()
                    .replace(new File(agentManagementService.getDeviceStoragePath()).getAbsolutePath(), "");
            pcImageRelPath =
                    agentManagementService.getDeviceFolderUrlPrefix() + pcImageRelPath.replace(File.separator, "/");
            deviceInfo.setPcImageRelPath(pcImageRelPath);
        }
        try {
            screenCapture(pcScreenShotImageFile.getAbsolutePath());
        } catch (IOException e) {
            classLogger.error("Screen capture failed for device: {}", deviceInfo, e);
        }
        StorageFileInfo fileInfo =
                new StorageFileInfo(pcScreenShotImageFile, "device/screenshots/" + pcScreenShotImageFile.getName(),
                        StorageFileInfo.FileType.SCREENSHOT, EntityType.SCREENSHOT);
        String fileDownloadUrl =
                agentManagementService.getStorageServiceClientProxy().upload(pcScreenShotImageFile, fileInfo)
                        .getBlobUrl();
        if (StringUtils.isBlank(fileDownloadUrl)) {
            classLogger.warn("Screenshot download url is empty for device {}", deviceInfo.getName());
        } else {
            deviceInfo.setPcScreenshotImageUrl(fileDownloadUrl);
        }
        return joinImages(pcScreenShotImageFile, deviceFile, deviceInfo.getName() + "-" + deviceInfo.getSerialNum() + "-" + "comb" + ".jpg");
    }

    public void screenCapture(String outputFile) throws IOException {
        File scrFile = appiumServerManager.getWindowsRootDriver(classLogger).getScreenshotAs(OutputType.FILE);
        BufferedImage screenshot = ImageIO.read(scrFile);
        ImgUtil.scale(screenshot, new File(outputFile), 0.7f);
    }

    @Override
    public ScreenRecorder getScreenRecorder(DeviceInfo deviceInfo, File folder, Logger logger) {
        return new AppiumE2ETestRecorder(this, this.adbOperateUtil, deviceInfo, folder, logger);
    }

    public File joinImages(File PCFile, File PhoneFile, String outFileName) {
        File outFile = new File(agentManagementService.getScreenshotDir(), outFileName);
        try {
            BufferedImage image_pc = ImageIO.read(PCFile);
            int width_pc = image_pc.getWidth();
            int height_pc = image_pc.getHeight();
            int[] imageArrayPC = new int[width_pc * height_pc];
            imageArrayPC = image_pc.getRGB(0, 0, width_pc, height_pc, imageArrayPC, 0, width_pc);

            BufferedImage image_phone = ImageIO.read(PhoneFile);
            int width_phone = image_phone.getWidth();
            int height_phone = image_phone.getHeight();
            int[] ImageArrayPhone = new int[width_phone * height_phone];
            ImageArrayPhone = image_phone.getRGB(0, 0, width_phone, height_phone, ImageArrayPhone, 0, width_phone);

            int height_new = Math.max(height_pc, height_phone);
            BufferedImage imageNew = new BufferedImage(width_pc + width_phone, height_new, BufferedImage.TYPE_INT_RGB);
            imageNew.setRGB(0, 0, width_pc, height_pc, imageArrayPC, 0, width_pc);
            imageNew.setRGB(width_pc, 0, width_phone, height_phone, ImageArrayPhone, 0, width_phone);

            ImageIO.write(imageNew, "jpg", outFile);
        } catch (Exception e) {
            e.printStackTrace();
            return PCFile;
        }
        return outFile;
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
