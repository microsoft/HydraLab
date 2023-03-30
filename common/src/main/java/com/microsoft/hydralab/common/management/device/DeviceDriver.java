package com.microsoft.hydralab.common.management.device;

import com.microsoft.hydralab.common.entity.common.AgentUser;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.management.AppiumServerManager;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;

import java.io.File;

public interface DeviceDriver {


    File getScreenShot(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger);

    File getScreenShotWithStrategy(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger, @NotNull AgentUser.BatteryStrategy batteryStrategy);

    void wakeUpDevice(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger);

    void backToHome(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger);

    void grantPermission(@NotNull DeviceInfo deviceInfo, @NotNull String packageName,
                         @NotNull String permissionName, @Nullable Logger logger);

    void addToBatteryWhiteList(@NotNull DeviceInfo deviceInfo, @NotNull String packageName,
                               @NotNull Logger logger);

    boolean installApp(@NotNull DeviceInfo deviceInfo, @NotNull String packagePath, @Nullable Logger logger);

    boolean uninstallApp(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger);

    void resetPackage(@NotNull DeviceInfo deviceInfo, @NotNull String packageName,
                      @Nullable Logger logger);

    void pushFileToDevice(@NotNull DeviceInfo deviceInfo, @NotNull String pathOnAgent,
                          @NotNull String pathOnDevice, @Nullable Logger logger) throws Exception;

    void pullFileFromDevice(@NotNull DeviceInfo deviceInfo, @NotNull String pathOnDevice,
                            @Nullable Logger logger) throws Exception;

    ScreenRecorder getScreenRecorder(@NotNull DeviceInfo deviceInfo, @NotNull File folder,
                                     @Nullable Logger logger);

    boolean grantAllTaskNeededPermissions(@NotNull DeviceInfo deviceInfo, @NotNull TestTask task,
                                          @Nullable Logger logger);

    boolean grantAllPackageNeededPermissions(@NotNull DeviceInfo deviceInfo, @NotNull File packageFile,
                                             @NotNull String targetPackage,
                                             boolean allowCustomizedPermissions, @Nullable Logger logger);

    Logger getDeviceLogger(DeviceInfo device);


    void updateAllDeviceInfo();

    LogCollector getLogCollector(@NotNull DeviceInfo deviceInfo, @NotNull String pkgName,
                                 @NotNull TestRun testRun, @NotNull Logger logger);

    void setProperty(@NotNull DeviceInfo deviceInfo, @NotNull String property, String val,
                     @Nullable Logger logger);

    boolean setDefaultLauncher(@NotNull DeviceInfo deviceInfo, @NotNull String packageName,
                               @NotNull String defaultActivity, @Nullable Logger logger);

    boolean isAppInstalled(@NotNull DeviceInfo deviceInfo, @NotNull String packageName,
                           @Nullable Logger logger);

    boolean grantProjectionAndBatteryPermission(@NotNull DeviceInfo deviceInfo,
                                                @NotNull String recordPackageName,
                                                @Nullable Logger logger);

    void testDeviceSetup(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger);

    void removeFileInDevice(DeviceInfo deviceInfo, String pathOnDevice, Logger logger);

    void testDeviceUnset(DeviceInfo deviceInfo, Logger logger);

    WebDriver getAppiumDriver(DeviceInfo deviceInfo, Logger logger);

    void quitAppiumDriver(DeviceInfo deviceInfo, Logger logger);

    void execCommandOnDevice(DeviceInfo deviceInfo, String command, Logger logger);

    void execCommandOnAgent(DeviceInfo deviceInfo, String command, Logger logger);

    AppiumServerManager getAppiumServerManager();

    void init();
}
