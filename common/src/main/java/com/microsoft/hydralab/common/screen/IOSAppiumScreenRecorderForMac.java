// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.screen;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.device.DeviceDriver;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.IOSUtils;
import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.common.util.ThreadUtils;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * iOS screen recorder for Mac using ffmpeg with pymobiledevice3 MJPEG port forwarding.
 * This is more reliable than Appium's built-in recording which has compatibility issues.
 */
public class IOSAppiumScreenRecorderForMac extends IOSAppiumScreenRecorder {
    private final Timer timer = new Timer();
    private Process recordProcess;
    private String destPath;

    public IOSAppiumScreenRecorderForMac(DeviceDriver deviceDriver, DeviceInfo info, String recordDir) {
        super(deviceDriver, info, recordDir);
    }

    @Override
    public void startRecord(int maxTimeInSecond) {
        int timeout = maxTimeInSecond > 0 ? maxTimeInSecond : DEFAULT_TIMEOUT_IN_SECOND;
        destPath = new File(recordDir, Const.ScreenRecoderConfig.DEFAULT_FILE_NAME).getAbsolutePath();
        try {
            // Get MJPEG port with pymobiledevice3 forwarding
            int mjpegPort = IOSUtils.getMjpegServerPortByUdid(deviceInfo.getSerialNum(), CLASS_LOGGER, deviceInfo);
            CLASS_LOGGER.info("Starting ffmpeg recording from MJPEG port {} to {}", mjpegPort, destPath);
            
            // Use ffmpeg to record from MJPEG stream with reconnect options for stability
            String ffmpegCommand;
            if (IOSUtils.isIOS17OrAbove(deviceInfo.getOsVersion())) {
                // iOS 17+: fix aspect ratio (scale=720:-2 auto-calculates height), set square
                // pixels (setsar=1), use standard yuv420p color space for QuickTime compatibility
                ffmpegCommand = String.format(
                    "ffmpeg -f mjpeg -reconnect 1 -reconnect_at_eof 1 -reconnect_streamed 1 -reconnect_delay_max %d -i http://127.0.0.1:%d -vf \"scale=720:-2,setsar=1\" -pix_fmt yuv420p -vcodec libx264 -y \"%s\"",
                    timeout + 1, mjpegPort, destPath
                );
                CLASS_LOGGER.info("iOS 17+: using QuickTime-compatible ffmpeg settings");
            } else {
                // iOS < 17: existing verified command
                ffmpegCommand = String.format(
                    "ffmpeg -f mjpeg -reconnect 1 -reconnect_at_eof 1 -reconnect_streamed 1 -reconnect_delay_max %d -i http://127.0.0.1:%d -vf scale=720:360 -vcodec h264 -y \"%s\"",
                    timeout + 1, mjpegPort, destPath
                );
            }
            recordProcess = ShellUtils.execLocalCommand(ffmpegCommand, false, CLASS_LOGGER);
            deviceInfo.addCurrentProcess(recordProcess);
            
            // Set up auto-stop timer
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    stopRecord();
                }
            }, timeout * 1000L);
            
            isStarted = true;
        } catch (Throwable e) {
            System.out.println("-------------------------------Fail to Start recording, Ignore it to unblocking the following tests----------------------------");
            e.printStackTrace();
            System.out.println("-------------------------------------------------------Ignore End--------------------------------------------------------------");
        }
    }

    @Override
    public String finishRecording() {
        timer.cancel();
        return stopRecord();
    }

    private String stopRecord() {
        if (!isStarted) {
            return null;
        }
        try {
            // wait 5s to record more info after testing
            ThreadUtils.safeSleep(5000);
            CLASS_LOGGER.info("Stopping ffmpeg recording");
            synchronized (this) {
                if (recordProcess != null && recordProcess.isAlive()) {
                    // Send SIGINT (Ctrl+C) to ffmpeg for graceful shutdown
                    // On Mac/Unix, we can use kill -INT
                    long pid = recordProcess.pid();
                    ShellUtils.execLocalCommand("kill -INT " + pid, CLASS_LOGGER);
                    // Wait for ffmpeg to finish writing
                    boolean finished = recordProcess.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
                    if (!finished) {
                        CLASS_LOGGER.warn("FFmpeg did not finish gracefully, force killing");
                        recordProcess.destroyForcibly();
                    }
                    recordProcess = null;
                }
                // Release MJPEG port forwarding
                IOSUtils.releaseMjpegServerPortByUdid(deviceInfo.getSerialNum(), CLASS_LOGGER);
                isStarted = false;
            }
        } catch (Throwable e) {
            System.out.println("-------------------------------Fail to Stop recording, Ignore it to unblocking the following tests-----------------------------");
            e.printStackTrace();
            System.out.println("-------------------------------------------------------Ignore End--------------------------------------------------------------");
            return null;
        }
        return destPath;
    }
}
