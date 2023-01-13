// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.screen;

import com.android.ddmlib.InstallException;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.logger.MultiLineNoCancelLoggingReceiver;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.FlowUtil;
import com.microsoft.hydralab.common.util.ThreadUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.*;
import java.util.Objects;


public class PhoneAppScreenRecorder implements ScreenRecorder {
    public static final String recordPackageName = "com.microsoft.hydralab.android.client";
    static boolean needInstall = true;
    private static File recordApk;
    protected final File baseFolder;
    protected final Logger logger;
    private final DeviceInfo deviceInfo;
    private final DeviceManager deviceManager;
    /* Send adb signal every 30s */
    public int preSleepSeconds = 30;
    protected String fileName = Const.ScreenRecoderConfig.DEFAULT_FILE_NAME;
    boolean started = false;
    ADBOperateUtil adbOperateUtil;
    private Thread keepAliveThread;

    public PhoneAppScreenRecorder(DeviceManager deviceManager, ADBOperateUtil adbOperateUtil, DeviceInfo deviceInfo, File baseFolder, Logger logger) {
        this.deviceManager = deviceManager;
        this.adbOperateUtil = adbOperateUtil;
        this.deviceInfo = deviceInfo;
        this.baseFolder = baseFolder;
        this.logger = logger;
    }

    public static void copyAPK(File testBaseDir) {
        // copy apk
        String name = "record_release.apk";
        recordApk = new File(testBaseDir, name);
        if (recordApk.exists()) {
            recordApk.delete();
        }
        try (InputStream resourceAsStream = FileUtils.class.getClassLoader().getResourceAsStream(name); OutputStream out = new FileOutputStream(recordApk)) {
            IOUtils.copy(Objects.requireNonNull(resourceAsStream), out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setupDevice() {
        if (needInstall || !deviceManager.isAppInstalled(deviceInfo, recordPackageName, logger)) {
            try {
                deviceManager.wakeUpDevice(deviceInfo, logger);
                deviceManager.uninstallApp(deviceInfo, recordPackageName, logger);
                deviceManager.installApp(deviceInfo, recordApk.getAbsolutePath(), logger);
                installRecorderServiceApp();

                deviceManager.grantAllPackageNeededPermissions(deviceInfo, recordApk, recordPackageName, false, logger);
                deviceManager.grantPermission(deviceInfo, recordPackageName, "android.permission.FOREGROUND_SERVICE", logger);
                deviceManager.addToBatteryWhiteList(deviceInfo, recordPackageName, logger);
                needInstall = false;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        FlowUtil.retryWhenFalse(3, () -> deviceManager.grantProjectionAndBatteryPermission(deviceInfo, recordPackageName, logger));
    }

    @Override
    public void startRecord(int maxTimeInSecond) {
        if (started) {
            return;
        }
        started = true;
        // am startservice --es fileName test.mp4 com.microsoft.hydralab.android.client/.ScreenRecorderService
        startRecordService();

        keepAliveThread = new Thread(() -> {
            try {
                if (preSleepSeconds > 0) {
                    int totalTime = 0;
                    while (totalTime < maxTimeInSecond) {
                        Thread.sleep(preSleepSeconds * 1000L);
                        sendKeepAliveSignal();
                        totalTime += preSleepSeconds;
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("InterruptedException from keepAliveThread {} {}", e.getClass().getName(), e.getMessage());
            }
        });
        keepAliveThread.start();
    }

    @Override
    public boolean finishRecording() {
        logger.info("finishRecording :" + started);
        if (!started) {
            return false;
        }
        boolean tag = false;
        // wait 5s to record more info after testing
        ThreadUtils.safeSleep(5000);
        stopRecordService();
        if (keepAliveThread != null) {
            if (!keepAliveThread.isInterrupted()) {
                keepAliveThread.interrupt();
            }
        }
        String pathOnDevice = "/sdcard/Movies/test_lab/" + fileName;
        String pathOnAgent = baseFolder.getAbsolutePath() + "/" + fileName;
        // wait for screen recording to finish
        ThreadUtils.safeSleep(5000);

        int retryTime = 1;
        while (retryTime < Const.AgentConfig.RETRY_TIME) {
            logger.info("Pull file round :" + retryTime);
            File videoFile = new File(pathOnAgent);
            if (videoFile.exists()) {
                videoFile.delete();
            }
            adbOperateUtil.pullFileToDir(deviceInfo, pathOnAgent, pathOnDevice, logger);
            ThreadUtils.safeSleep(5000);

            long phoneFileSize = adbOperateUtil.getFileLength(deviceInfo, logger, pathOnDevice);
            logger.info("PC file path:{} size:{} , Phone file path {} size {}", pathOnAgent, videoFile.length(), pathOnDevice, phoneFileSize);
            if (videoFile.length() == phoneFileSize) {
                logger.info("Pull video file success!");
                tag = true;
                break;
            }
            retryTime++;
            if (retryTime == Const.AgentConfig.RETRY_TIME) {
                logger.error("Pull video file fail!");
            }
        }
        deviceManager.removeFileInDevice(deviceInfo, pathOnDevice, logger);
        started = false;
        return tag;
    }

    @Override
    public int getPreSleepSeconds() {
        return 0;
    }

    public boolean startRecordService() {
        try {
            // am startservice --es fileName test.mp4 com.microsoft.hydralab.android.client/.ScreenRecorderService
            adbOperateUtil.execOnDevice(deviceInfo, String.format("am startservice -a %s.action.START --es fileName %s --es SNCode %s --ei width 720 --ei bitrate 1200000 %s/.ScreenRecorderService", recordPackageName, fileName, deviceInfo.getSerialNum(), recordPackageName), new MultiLineNoCancelLoggingReceiver(logger), logger);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean stopRecordService() {
        try {
            adbOperateUtil.execOnDevice(deviceInfo, "am startservice -a " + recordPackageName + ".action.STOP " + recordPackageName + "/.ScreenRecorderService", new MultiLineNoCancelLoggingReceiver(logger), logger);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean sendKeepAliveSignal() {

        try {
            adbOperateUtil.execOnDevice(deviceInfo, "am startservice -a " + recordPackageName + ".action.SIGNAL " + recordPackageName + "/.ScreenRecorderService", new MultiLineNoCancelLoggingReceiver(logger), logger);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    private void installRecorderServiceApp() throws InstallException {
        try {
            deviceManager.installApp(deviceInfo, recordApk.getAbsolutePath(), logger);
        } catch (InstallException e) {
            // if failed to install app, uninstall app and try again.
            logger.error(e.getMessage(), e);
            deviceManager.uninstallApp(deviceInfo, recordPackageName, logger);
            deviceManager.installApp(deviceInfo, recordApk.getAbsolutePath(), logger);
        }
    }
}
