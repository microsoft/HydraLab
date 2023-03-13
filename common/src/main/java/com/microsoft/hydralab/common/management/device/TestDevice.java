// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.management.device;

import com.android.ddmlib.InstallException;
import com.android.ddmlib.TimeoutException;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.ThreadPoolUtil;
import com.microsoft.hydralab.common.util.ThreadUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

public class TestDevice implements TestDeviceOperation {
    private final DeviceInfo deviceInfo;
    private final TestDeviceTag tag;

    private ScreenRecorder screenRecorder;

    private LogCollector logCollector;

    public TestDevice(DeviceInfo deviceInfo, TestDeviceTag tag) {
        this.deviceInfo = deviceInfo;
        this.tag = tag;
    }

    public TestDeviceTag getTag() {
        return tag;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public File getScreenShot(@Nullable Logger logger) throws Exception {
        return deviceInfo.getTestDeviceManager().getScreenShot(deviceInfo, logger);
    }

    @Override
    public void wakeUpDevice(@Nullable Logger logger) {
        deviceInfo.getTestDeviceManager().wakeUpDevice(deviceInfo, logger);
    }

    @Override
    public boolean installApp(@NotNull String packagePath, @Nullable Logger logger) throws InstallException {
        return deviceInfo.getTestDeviceManager().installApp(deviceInfo, packagePath, logger);
    }

    @Override
    public boolean uninstallApp(@NotNull String packageName, @Nullable Logger logger) throws InstallException {
        return deviceInfo.getTestDeviceManager().uninstallApp(deviceInfo, packageName, logger);
    }

    @Override
    public void resetPackage(@NotNull String packageName, @Nullable Logger logger) {
        deviceInfo.getTestDeviceManager().resetPackage(deviceInfo, packageName, logger);
    }

    @Override
    public void startScreenRecorder(@NotNull File folder, int maxTimeInSecond, @Nullable Logger logger) {
        screenRecorder = deviceInfo.getTestDeviceManager().getScreenRecorder(deviceInfo, folder, logger);
        screenRecorder.setupDevice();
        screenRecorder.startRecord(maxTimeInSecond);
    }

    @Override
    public void stopScreenRecorder() {
        screenRecorder.finishRecording();
    }

    @Override
    public String startLogCollector(@NotNull String pkgName, @NotNull TestRun testRun, @NotNull Logger logger) {
        logCollector = deviceInfo.getTestDeviceManager().getLogCollector(deviceInfo, pkgName, testRun, logger);
        return logCollector.start();
    }

    @Override
    public void stopLogCollector() {
        logCollector.stopAndAnalyse();
    }

    @Override
    public void testDeviceSetup(@Nullable Logger logger) throws IOException {
        deviceInfo.getTestDeviceManager().testDeviceSetup(deviceInfo, logger);
    }

    @Override
    public void testDeviceUnset(Logger logger) {
        deviceInfo.getTestDeviceManager().testDeviceUnset(deviceInfo, logger);
    }

    @Override
    public void setRunningTestName(String runningTestName) {
        deviceInfo.setRunningTestName(runningTestName);
    }

    @Override
    public String getName() {
        return deviceInfo.getName();
    }

    @Override
    public String getSerialNum() {
        return deviceInfo.getSerialNum();
    }

    @Override
    public String getOsVersion() {
        return deviceInfo.getOsVersion();
    }

    @Override
    public void killAll() {
        deviceInfo.killAll();
    }

    @Override
    public void addCurrentTask(TestTask testTask) {
        deviceInfo.addCurrentTask(testTask);
    }

    @Override
    public void finishTask() {
        deviceInfo.finishTask();
    }

    @Override
    public void quitMobileAppiumDriver(Logger logger) {
        deviceInfo.getTestDeviceManager().quitMobileAppiumDriver(deviceInfo, logger);
    }

    @Override
    public void updateScreenshotImageAsyncDelay(long delayMillis, @NotNull FileAvailableCallback fileAvailableCallback, @NotNull Logger logger) {

        ThreadPoolUtil.SCREENSHOT_EXECUTOR.execute(() -> {
            try {
                ThreadUtils.safeSleep(delayMillis);
                File imageFile = getScreenShot(logger);
                if (fileAvailableCallback != null) {
                    fileAvailableCallback.onFileReady(imageFile);
                }
            } catch (TimeoutException te) {
                logger.error("{}: {}, updateScreenshotImageAsyncDelay", te.getClass().getSimpleName(),
                        te.getMessage());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    @Override
    public void grantAllTaskNeededPermissions(@NotNull TestTask testTask, @Nullable Logger logger) {
        deviceInfo.getTestDeviceManager().grantAllTaskNeededPermissions(deviceInfo, testTask, logger);
    }

    @Override
    public void runAppiumMonkey(String packageName, int round, Logger logger) {
        deviceInfo.getTestDeviceManager().runAppiumMonkey(deviceInfo, packageName, round, logger);
    }

    @Override
    public boolean runAppiumT2CTest(File jsonFile, Logger reportLogger) {
        return deviceInfo.getTestDeviceManager().runAppiumT2CTest(deviceInfo, jsonFile, reportLogger);
    }

}
