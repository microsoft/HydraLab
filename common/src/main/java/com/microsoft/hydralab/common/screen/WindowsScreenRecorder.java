// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.screen;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.device.DeviceDriver;
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

public class WindowsScreenRecorder implements ScreenRecorder {
    private final DeviceDriver deviceDriver;
    private final DeviceInfo deviceInfo;
    private final File baseFolder;
    private final Logger logger;
    private WindowsDriver windowsDriver;
    private boolean started = false;

    public WindowsScreenRecorder(DeviceDriver deviceDriver, DeviceInfo deviceInfo, File baseFolder, Logger logger) {
        this.deviceDriver = deviceDriver;
        this.deviceInfo = deviceInfo;
        this.baseFolder = baseFolder;
        this.logger = logger;
        windowsDriver = deviceDriver.getAppiumServerManager().getWindowsRootDriver(logger);
    }

    @Override
    public void startRecord(int maxTime) {
        logger.info("Start phone record screen");
        logger.info("Start PC record screen");
        windowsDriver.startRecordingScreen(new WindowsStartScreenRecordingOptions().withTimeLimit(Duration.ofSeconds(maxTime)));
        started = true;
    }

    @Override
    public void setupDevice() {
        // Do nothing
    }

    @Override
    public String finishRecording() {
        if (!started) {
            return null;
        }

        File PCVideoFile = null;
        try {
            String base64String = windowsDriver.stopRecordingScreen();
            byte[] data = Base64.getDecoder().decode(base64String);
            PCVideoFile = new File(baseFolder.getAbsolutePath(), Const.ScreenRecoderConfig.PC_FILE_NAME);
            Path path = Paths.get(PCVideoFile.getAbsolutePath());
            Files.write(path, data);
        } catch (Throwable e) {
            System.out.println("-------------------------------Fail to Stop recording, Ignore it to unblocking the following tests-----------------------------");
            e.printStackTrace();
            System.out.println("-------------------------------------------------------Ignore End--------------------------------------------------------------");
        }

        started = false;

        if (PCVideoFile == null || !PCVideoFile.exists()) {
            return null;
        }
        return PCVideoFile.getAbsolutePath();
    }

    @Override
    public int getPreSleepSeconds() {
        return 0;
    }
}