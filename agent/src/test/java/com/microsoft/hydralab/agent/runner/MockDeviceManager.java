// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner;

import com.android.ddmlib.InstallException;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.logger.impl.ADBLogcatCollector;
import com.microsoft.hydralab.common.management.impl.AndroidDeviceManager;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * @author zhoule
 * @date 12/20/2022
 */

public class MockDeviceManager extends AndroidDeviceManager {
    @Override
    public void init() throws Exception {

    }

    @Override
    public Set<DeviceInfo> getDeviceList(@Nullable Logger logger) {
        return null;
    }

    @Override
    public Set<DeviceInfo> getActiveDeviceList(@Nullable Logger logger) {
        return null;
    }

    @Override
    public File getScreenShot(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) throws Exception {
        return null;
    }

    @Override
    public void resetDeviceByTestId(@NotNull String testId, @Nullable Logger logger) {

    }

    @Override
    public void wakeUpDevice(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) {

    }

    @Override
    public void backToHome(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) {
        logger.info("Invoke setProperty success! deviceInfo {}", deviceInfo);
    }

    @Override
    public void grantPermission(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull String permissionName, @Nullable Logger logger) {

    }

    @Override
    public void addToBatteryWhiteList(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull Logger logger) {

    }

    @Override
    public boolean installApp(@NotNull DeviceInfo deviceInfo, @NotNull String packagePath, @Nullable Logger logger) throws InstallException {
        return false;
    }

    @Override
    public boolean uninstallApp(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger) throws InstallException {
        return false;
    }

    @Override
    public void resetPackage(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger) {

    }

    @Override
    public ScreenRecorder getScreenRecorder(@NotNull DeviceInfo deviceInfo, @NotNull File folder, @Nullable Logger logger) {
        return null;
    }

    @Override
    public ADBLogcatCollector getLogCollector(@NotNull DeviceInfo deviceInfo, @NotNull String pkgName, @NotNull DeviceTestTask deviceTestResult, @NotNull Logger logger) {
        return null;
    }

    @Override
    public void setProperty(@NotNull DeviceInfo deviceInfo, @NotNull String property, String val, @Nullable Logger logger) {
        logger.info("Invoke setProperty success! deviceInfo {}, property {}, val {}", deviceInfo, property, val);
    }

    @Override
    public void updateIsPrivateByDeviceSerial(@NotNull String deviceSerial, @NotNull Boolean isPrivate) {

    }

    @Override
    public boolean setDefaultLauncher(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull String defaultActivity, @Nullable Logger logger) {
        logger.info("Invoke setDefaultLauncher success! deviceInfo {}, packageName {}, defaultActivity {}", deviceInfo, packageName, defaultActivity);
        return true;
    }

    @Override
    public boolean isAppInstalled(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger) {
        return false;
    }

    @Override
    public boolean grantProjectionAndBatteryPermission(@NotNull DeviceInfo deviceInfo, @NotNull String recordPackageName, @Nullable Logger logger) {
        return false;
    }

    @Override
    public void testDeviceSetup(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) throws IOException {

    }

    @Override
    public void removeFileInDevice(DeviceInfo deviceInfo, String pathOnDevice, Logger logger) {

    }

    @Override
    public void testDeviceUnset(DeviceInfo deviceInfo, Logger logger) {

    }

    @Override
    public WebDriver getMobileAppiumDriver(DeviceInfo deviceInfo, Logger logger) {
        return null;
    }

    @Override
    public void quitMobileAppiumDriver(DeviceInfo deviceInfo, Logger logger) {

    }

    private void changeGlobalSetting(DeviceInfo deviceInfo, String property, String val, Logger logger) {
        logger.info("Invoke changeGlobalSetting success! property {}, val {}, defaultActivity {}", deviceInfo, property, val);
    }

    private void changeSystemSetting(DeviceInfo deviceInfo, String property, String val, Logger logger) {
        logger.info("Invoke changeSystemSetting success! property {}, val {}, defaultActivity {}", deviceInfo, property, val);
    }
}
