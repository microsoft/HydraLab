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
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Base64;

public class IOSAppiumScreenRecorderForMac extends IOSAppiumScreenRecorder {

    public IOSAppiumScreenRecorderForMac(DeviceDriver deviceDriver, DeviceInfo info, String recordDir) {
        super(deviceDriver, info, recordDir);
    }

    @Override
    public void startRecord(int maxTimeInSecond) {
        int timeout = maxTimeInSecond > 0 ? maxTimeInSecond : DEFAULT_TIMEOUT_IN_SECOND;
        try {
            FlowUtil.retryAndSleepWhenFalse(3, 1000, () -> {
                iosDriver.startRecordingScreen(new IOSStartScreenRecordingOptions()
                        .enableForcedRestart()
                        .withFps(24)
                        .withVideoType("h264")
                        .withVideoScale("720:360")
                        .withTimeLimit(Duration.ofSeconds(timeout)));
                return true;
            });
            isStarted = true;
        } catch (Exception e) {
            System.out.println("-------------------------------Fail to Start recording, Ignore it to unblocking the following tests----------------------------");
            e.printStackTrace();
            System.out.println("-------------------------------------------------------Ignore End--------------------------------------------------------------");
        }
    }

    @Override
    public String finishRecording() {
        if (!isStarted) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss");
        String destPath = "";
        try {
            // wait 5s to record more info after testing
            ThreadUtils.safeSleep(5000);
            String base64String = iosDriver.stopRecordingScreen();
            byte[] data = Base64.getDecoder().decode(base64String);
            destPath = new File(recordDir, Const.ScreenRecoderConfig.DEFAULT_FILE_NAME).getAbsolutePath();
            Path path = Paths.get(destPath);
            Files.write(path, data);
            isStarted = false;
        } catch (Throwable e) {
            System.out.println("-------------------------------Fail to Stop recording, Ignore it to unblocking the following tests-----------------------------");
            e.printStackTrace();
            System.out.println("-------------------------------------------------------Ignore End--------------------------------------------------------------");
            return null;
        }
        return destPath;
    }
}
