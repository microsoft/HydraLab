// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.screen;

import cn.hutool.core.thread.ThreadUtil;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
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
    private final Object lock = new Object();
    private final DeviceInfo deviceInfo;
    private final Logger logger;
    private final File baseFolder;
    private int preSleepSeconds = 0;
    ADBOperateUtil adbOperateUtil;
    private Process recordingProcess;
    private Thread recordingThread;
    private boolean shouldStop = true;
    private boolean shouldInterrupt = false;

    public ADBScreenRecorder(ADBOperateUtil adbOperateUtil, DeviceInfo deviceInfo, Logger logger, File baseFolder) {
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
                    String fileName = String.format("/sdcard/scr_rec_%d_%d.mp4", totalTime, totalTime + timeSpan);
                    String command = String.format("shell screenrecord --bit-rate 3200000 --time-limit %d %s", timeSpan, fileName);
                    deviceInfo.addCurrentCommand(command);
                    // Blocking command
                    recordingProcess = adbOperateUtil.executeDeviceCommandOnPC(deviceInfo, command, logger);
                    logger.info("ADBDeviceScreenRecorder>> command: " + command);
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

                    String outputFilePrefix = new File(baseFolder, DateUtil.FILE_NAME_DATE_DASH_FORMAT.format(new Date())).getAbsolutePath();

                    final String outFileFullPath = outputFilePrefix + "_" + totalTime + "_" + (totalTime + timeSpan) + ".mp4";
                    String pullComm = String.format("pull %s %s", fileName, outFileFullPath);
                    Process process = adbOperateUtil.executeDeviceCommandOnPC(deviceInfo, pullComm, logger);

                    logger.info(IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8));
                    logger.error(IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8));
                    process.destroy();

                    list.add(new File(outFileFullPath));

                    totalTime += timeSpan;
                    logger.info("ADBDeviceScreenRecorder>> Time recorded {}", totalTime);
                }

                shouldInterrupt = false;
                final File mergedVideo = FFmpegConcatUtil.concatVideos(list, baseFolder, logger);
                ThreadUtil.safeSleep(2000);
                if (mergedVideo != null && mergedVideo.exists()) {
                    logger.info("deleting merged old videos " + list);
                    list.forEach(File::delete);
                }

            } catch (IOException e) {
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
    public boolean finishRecording() {
        if (shouldStop) {
            return false;
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
            return false;
        }
        logger.info("Complete waiting: {}", (System.currentTimeMillis() - time) / 1000f);
        return true;
    }

}
