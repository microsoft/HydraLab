// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.screen;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.logger.MultiLineNoCancelLoggingReceiver;
import com.microsoft.hydralab.common.management.device.DeviceDriver;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.FlowUtil;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.ThreadUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;


public class PhoneAppScreenRecorder implements ScreenRecorder {
    public static final String RECORD_PACKAGE_NAME = "com.microsoft.hydralab.android.client";
    private static File recordApk;
    protected final File baseFolder;
    protected final Logger logger;
    private final DeviceInfo deviceInfo;
    private final DeviceDriver deviceDriver;
    /* Send adb signal every 30s */
    public int preSleepSeconds = 30;
    protected String fileName = Const.ScreenRecoderConfig.DEFAULT_FILE_NAME;
    boolean started = false;
    ADBOperateUtil adbOperateUtil;
    private Thread keepAliveThread;

    public PhoneAppScreenRecorder(DeviceDriver deviceDriver, ADBOperateUtil adbOperateUtil, DeviceInfo deviceInfo, File baseFolder, Logger logger) {
        this.deviceDriver = deviceDriver;
        this.adbOperateUtil = adbOperateUtil;
        this.deviceInfo = deviceInfo;
        this.baseFolder = baseFolder;
        this.logger = logger;
    }

    public static void copyAPK(File preAppDir) {
        // copy apk
        String name = "record_release.apk";
        recordApk = new File(preAppDir, name);
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
        if (!deviceDriver.isAppInstalled(deviceInfo, RECORD_PACKAGE_NAME, logger)) {
            installRecorderServiceApp();
        }
        try {
            deviceDriver.wakeUpDevice(deviceInfo, logger);
            deviceDriver.unlockDevice(deviceInfo, logger);
            deviceDriver.grantAllPackageNeededPermissions(deviceInfo, recordApk, RECORD_PACKAGE_NAME, false, logger);
            deviceDriver.grantPermission(deviceInfo, RECORD_PACKAGE_NAME, "android.permission.FOREGROUND_SERVICE", logger);
            deviceDriver.addToBatteryWhiteList(deviceInfo, RECORD_PACKAGE_NAME, logger);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        FlowUtil.retryWhenFalse(3, () -> deviceDriver.grantProjectionAndBatteryPermission(deviceInfo, RECORD_PACKAGE_NAME, logger));
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
    public String finishRecording() {
        logger.info("finishRecording :" + started);
        if (!started) {
            return null;
        }
        boolean pullSucceeded = false;
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

        try {
            adbOperateUtil.pullFileToDir(deviceInfo, pathOnAgent, pathOnDevice, logger);
            pullSucceeded = true;
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        deviceDriver.removeFileInDevice(deviceInfo, pathOnDevice, logger);
        started = false;
        if (pullSucceeded) {
            return pathOnAgent;
        }
        return null;
    }

    @Override
    public int getPreSleepSeconds() {
        return 0;
    }

    public boolean startRecordService() {
        try {
            // am startservice --es fileName test.mp4 com.microsoft.hydralab.android.client/.ScreenRecorderService
            adbOperateUtil.execOnDevice(deviceInfo, String.format("am startservice -a %s.action.START --es fileName %s --es SNCode %s --ei width 720 --ei bitrate 1200000 %s/.ScreenRecorderService",
                    RECORD_PACKAGE_NAME, fileName, deviceInfo.getSerialNum(), RECORD_PACKAGE_NAME), new MultiLineNoCancelLoggingReceiver(logger), logger);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean stopRecordService() {
        try {
            adbOperateUtil.execOnDevice(deviceInfo, "am startservice -a " + RECORD_PACKAGE_NAME + ".action.STOP " + RECORD_PACKAGE_NAME + "/.ScreenRecorderService", new MultiLineNoCancelLoggingReceiver(logger), logger);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean sendKeepAliveSignal() {

        try {
            adbOperateUtil.execOnDevice(deviceInfo, "am startservice -a " + RECORD_PACKAGE_NAME + ".action.SIGNAL " + RECORD_PACKAGE_NAME + "/.ScreenRecorderService", new MultiLineNoCancelLoggingReceiver(logger), logger);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    private void installRecorderServiceApp() {
        try {
            deviceDriver.installApp(deviceInfo, recordApk.getAbsolutePath(), logger);
        } catch (HydraLabRuntimeException e) {
            // if failed to install app, uninstall app and try again.
            logger.error(e.getMessage(), e);
            deviceDriver.uninstallApp(deviceInfo, RECORD_PACKAGE_NAME, logger);
            deviceDriver.installApp(deviceInfo, recordApk.getAbsolutePath(), logger);
        }
    }
}
