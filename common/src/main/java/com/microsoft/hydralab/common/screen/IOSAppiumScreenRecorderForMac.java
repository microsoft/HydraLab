// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.screen;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.device.DeviceDriver;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.FlowUtil;
import com.microsoft.hydralab.common.util.ThreadUtils;
import io.appium.java_client.ios.IOSStartScreenRecordingOptions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;

public class IOSAppiumScreenRecorderForMac extends IOSAppiumScreenRecorder {

    public IOSAppiumScreenRecorderForMac(DeviceDriver deviceDriver, DeviceInfo info, String recordDir) {
        super(deviceDriver, info, recordDir);
        CLASS_LOGGER.info("🎬 IOSAppiumScreenRecorderForMac initialized. Record dir: {}", recordDir);
    }

    @Override
    public void startRecord(int maxTimeInSecond) {
        if (!isDriverInitialized || iosDriver == null) {
            CLASS_LOGGER.error("❌ Cannot start recording - IOSDriver not initialized. Skipping video recording.");
            CLASS_LOGGER.error("💡 Ensure WDA (WebDriverAgent) is installed on the iOS device.");
            return;
        }

        int timeout = maxTimeInSecond > 0 ? maxTimeInSecond : DEFAULT_TIMEOUT_IN_SECOND;
        CLASS_LOGGER.info("🎬 Starting iOS screen recording for device: {} (timeout: {}s)", 
                deviceInfo.getSerialNum(), timeout);
        try {
            FlowUtil.retryAndSleepWhenFalse(3, 10, () -> {
                CLASS_LOGGER.info("📹 Calling iosDriver.startRecordingScreen() with 720p @ 30fps...");
                iosDriver.startRecordingScreen(new IOSStartScreenRecordingOptions()
                        .enableForcedRestart()
                        .withFps(30)              // 30 fps for smoother video
                        .withVideoType("h264")
                        .withVideoScale("1280:720") // 720p resolution (was 720:360)
                        .withVideoQuality(IOSStartScreenRecordingOptions.VideoQuality.HIGH)
                        .withTimeLimit(Duration.ofSeconds(timeout)));
                return true;
            });
            isStarted = true;
            CLASS_LOGGER.info("✅ iOS screen recording started successfully for device: {}", deviceInfo.getSerialNum());
        } catch (Exception e) {
            CLASS_LOGGER.error("❌ Failed to start iOS screen recording: {}", e.getMessage());
            CLASS_LOGGER.error("💡 Possible causes: WDA not running, Appium session expired, or device disconnected.");
            CLASS_LOGGER.debug("Stack trace:", e);
        }
    }

    @Override
    public String finishRecording() {
        if (!isStarted) {
            CLASS_LOGGER.warn("⚠️ finishRecording() called but recording was never started. Returning null.");
            return null;
        }

        if (iosDriver == null) {
            CLASS_LOGGER.error("❌ Cannot stop recording - IOSDriver is null.");
            isStarted = false;
            return null;
        }

        CLASS_LOGGER.info("⏹️ Stopping iOS screen recording for device: {}", deviceInfo.getSerialNum());
        String destPath = "";
        try {
            // wait 5s to record more info after testing
            CLASS_LOGGER.info("⏳ Waiting 5s before stopping recording...");
            ThreadUtils.safeSleep(5000);

            CLASS_LOGGER.info("📹 Calling iosDriver.stopRecordingScreen()...");
            String base64String = iosDriver.stopRecordingScreen();

            if (base64String == null || base64String.isEmpty()) {
                CLASS_LOGGER.error("❌ stopRecordingScreen() returned empty data.");
                isStarted = false;
                return null;
            }

            byte[] data = Base64.getDecoder().decode(base64String);
            destPath = new File(recordDir, Const.ScreenRecoderConfig.DEFAULT_FILE_NAME).getAbsolutePath();
            Path path = Paths.get(destPath);
            Files.write(path, data);
            isStarted = false;

            File videoFile = new File(destPath);
            if (videoFile.exists() && videoFile.length() > 0) {
                CLASS_LOGGER.info("✅ iOS screen recording saved successfully: {} ({}KB)", 
                        destPath, videoFile.length() / 1024);
            } else {
                CLASS_LOGGER.error("❌ Video file was not created or is empty: {}", destPath);
                return null;
            }
        } catch (Throwable e) {
            CLASS_LOGGER.error("❌ Failed to stop iOS screen recording: {}", e.getMessage());
            CLASS_LOGGER.error("💡 Possible causes: Recording timeout exceeded, WDA crashed, or device disconnected.");
            CLASS_LOGGER.debug("Stack trace:", e);
            isStarted = false;
            return null;
        }
        return destPath;
    }
}
