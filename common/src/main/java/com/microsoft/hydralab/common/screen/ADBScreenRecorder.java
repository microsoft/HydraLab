// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.screen;

import cn.hutool.core.thread.ThreadUtil;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.device.DeviceDriver;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.DateUtil;
import com.microsoft.hydralab.common.util.ThreadUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ADBScreenRecorder implements ScreenRecorder {
    public final Object lock = new Object();
    private final DeviceInfo deviceInfo;
    private final Logger logger;
    private final File baseFolder;

    private File mergedVideo;
    private final DeviceDriver deviceDriver;
    public int preSleepSeconds = 0;
    ADBOperateUtil adbOperateUtil;
    private Process recordingProcess;
    private Thread recordingThread;
    private boolean shouldStop = true;
    private boolean shouldInterrupt = false;

    public ADBScreenRecorder(DeviceDriver deviceDriver, ADBOperateUtil adbOperateUtil, DeviceInfo deviceInfo, Logger logger, File baseFolder) {
        this.deviceDriver = deviceDriver;
        this.adbOperateUtil = adbOperateUtil;
        this.deviceInfo = deviceInfo;
        this.logger = logger;
        this.baseFolder = baseFolder;
    }

    @Override
    public int getPreSleepSeconds() {
        return preSleepSeconds;
    }

    public void setPreSleepSeconds(int preSleepSeconds) {
        this.preSleepSeconds = preSleepSeconds;
    }

    @Override
    public void setupDevice() {
    }

    @Override
    public void startRecord(int maxTimeInSecond) {
        if (!shouldStop) {
            return;
        }
        shouldStop = false;
        recordingThread = new Thread(() -> {
            try {
                if (preSleepSeconds > 0) {
                    ThreadUtils.safeSleep(preSleepSeconds * 1000L);
                }
                int timeSpan = 180;
                if (maxTimeInSecond < timeSpan) {
                    timeSpan = maxTimeInSecond;
                }
                int totalTime = 0;
                List<File> list = new ArrayList<>();
                while (totalTime < maxTimeInSecond && !shouldStop) {
                    String pathOnDevice = String.format("/sdcard/scr_rec_%d_%d.mp4", totalTime, totalTime + timeSpan);
                    String recordCommand = String.format("shell screenrecord --bit-rate 3200000 --time-limit %d %s", timeSpan, pathOnDevice);
                    deviceInfo.addCurrentCommand(recordCommand);
                    // Blocking command
                    recordingProcess = adbOperateUtil.executeDeviceCommandOnPC(deviceInfo, recordCommand, logger);
                    logger.info("ADBDeviceScreenRecorder>> command: " + recordCommand);
                    logger.info(IOUtils.toString(recordingProcess.getInputStream(), StandardCharsets.UTF_8));
                    logger.error(IOUtils.toString(recordingProcess.getErrorStream(), StandardCharsets.UTF_8));
                    deviceInfo.addCurrentProcess(recordingProcess);

                    try {
                        logger.info("waiting for recording");
                        shouldInterrupt = true;
                        recordingProcess.waitFor(3, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        logger.warn("InterruptedException from recordingProcess.waitFor {} {}", e.getClass().getName(), e.getMessage());
                    }

                    if (recordingProcess.isAlive()) {
                        recordingProcess.destroy();
                    }
                    deviceInfo.finishCommand();
                    // make sure the recording procedure is stopped completely
                    ThreadUtil.safeSleep(2000);

                    final String outFileName = DateUtil.fileNameDateDashFormat.format(new Date()) + "_" + totalTime + "_" + (totalTime + timeSpan) + ".mp4";
                    String pathOnAgent = new File(baseFolder, outFileName).getAbsolutePath();
                    adbOperateUtil.pullFileToDir(deviceInfo, pathOnAgent, pathOnDevice, logger);
                    list.add(new File(pathOnAgent));
                    deviceDriver.removeFileInDevice(deviceInfo, pathOnDevice, logger);

                    totalTime += timeSpan;
                    logger.info("ADBDeviceScreenRecorder>> Time recorded {}", totalTime);
                }

                shouldInterrupt = false;
                mergedVideo = FFmpegConcatUtil.concatVideos(list, baseFolder, logger);
                ThreadUtil.safeSleep(2000);
                if (mergedVideo != null && mergedVideo.exists()) {
                    logger.info("deleting merged old videos " + list);
                    list.forEach(File::delete);
                }

            } catch (IOException | InterruptedException e) {
                logger.warn("Exception from recordingThread {} {}", e.getClass().getName(), e.getMessage());
            } finally {
                if (recordingProcess != null) {
                    if (recordingProcess.isAlive()) {
                        recordingProcess.destroy();
                    }
                }
                try {
                    lock.notifyAll();
                } catch (Exception e) {
                    logger.warn("Exception from notifyAll {} {}", e.getClass().getName(), e.getMessage());
                }
            }
        });
        recordingThread.start();
    }

    @Override
    public String finishRecording() {
        if (shouldStop) {
            return null;
        }
        shouldStop = true;
        if (recordingThread != null && shouldInterrupt) {
            if (!recordingThread.isInterrupted()) {
                recordingThread.interrupt();
            }
        }
        if (recordingProcess != null && recordingProcess.isAlive()) {
            recordingProcess.destroy();
        }
        logger.info("start to wait for recording finish");
        long time = System.currentTimeMillis();
        try {
            synchronized (lock) {
                lock.wait(TimeUnit.MINUTES.toMillis(2));
            }
        } catch (Exception e) {
            logger.warn("Exception from recordingThread {} {}", e.getClass().getName(), e.getMessage());
            return null;
        }
        logger.info("Complete waiting: {}", (System.currentTimeMillis() - time) / 1000f);
        return mergedVideo.getAbsolutePath();
    }

}
