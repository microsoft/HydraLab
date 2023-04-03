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
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.OutputType;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class WindowsTestDeviceManager extends AndroidTestDeviceManager {

    @Override
    public File getScreenShot(DeviceInfo deviceInfo, Logger logger) {
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
}
