// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.screen;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.Const;
import io.appium.java_client.windows.WindowsDriver;
import io.appium.java_client.windows.WindowsStartScreenRecordingOptions;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;

public class AppiumE2ETestRecorder extends PhoneAppScreenRecorder {
    private WindowsDriver windowsDriver;

    public AppiumE2ETestRecorder(DeviceManager deviceManager, ADBOperateUtil adbOperateUtil, DeviceInfo deviceInfo, File baseFolder, Logger logger) {
        super(deviceManager, adbOperateUtil, deviceInfo, baseFolder, logger);
        windowsDriver = deviceManager.getAppiumServerManager().getWindowsRootDriver(logger);
    }

    @Override
    public void startRecord(int maxTime) {
        super.fileName = Const.ScreenRecoderConfig.PHONE_FILE_NAME;
        logger.info("Start phone record screen");
        super.startRecord(maxTime);
        logger.info("Start PC record screen");
        windowsDriver.startRecordingScreen(new WindowsStartScreenRecordingOptions().withTimeLimit(Duration.ofSeconds(maxTime)));

    }

    @Override
    public void setupDevice() {
        super.setupDevice();
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public boolean finishRecording() {
        File pcVideofile = null;
        File phoneVideoFile = null;
        super.finishRecording();
        try {
            String base64String = windowsDriver.stopRecordingScreen();
            byte[] data = Base64.getDecoder().decode(base64String);
            pcVideofile = new File(baseFolder.getAbsolutePath(), Const.ScreenRecoderConfig.PC_FILE_NAME);
            Path path = Paths.get(pcVideofile.getAbsolutePath());
            Files.write(path, data);
        } catch (Throwable e) {
            System.out.println("-------------------------------Fail to Stop recording, Ignore it to unblocking the following tests-----------------------------");
            e.printStackTrace();
            System.out.println("-------------------------------------------------------Ignore End--------------------------------------------------------------");
        }

        phoneVideoFile = new File(baseFolder.getAbsolutePath(), Const.ScreenRecoderConfig.PHONE_FILE_NAME);

        if (pcVideofile == null || !pcVideofile.exists() || !phoneVideoFile.exists()) {
            return false;
        }
        // Merge two videos side-by-side if exist
        System.out.println("-------------Merge two videos side-by-side-------------");
        String mergeDestinationPath = new File(baseFolder.getAbsolutePath(), Const.ScreenRecoderConfig.DEFAULT_FILE_NAME).getAbsolutePath();
        FFmpegConcatUtil.mergeVideosSideBySide(phoneVideoFile.getAbsolutePath(), pcVideofile.getAbsolutePath(), mergeDestinationPath, logger);
        // PCVideoFile.delete();
        // phoneVideoFile.delete();
        return true;
    }

}
