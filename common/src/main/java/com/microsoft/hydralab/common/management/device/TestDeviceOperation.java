// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.management.device;

import com.android.ddmlib.InstallException;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

interface TestDeviceOperation {
    File getScreenShot(@Nullable Logger logger) throws Exception;

    void wakeUpDevice(@Nullable Logger logger);

    boolean installApp(@NotNull String packagePath, @Nullable Logger logger) throws InstallException;

    boolean uninstallApp(@NotNull String packageName, @Nullable Logger logger) throws InstallException;

    void resetPackage(@NotNull String packageName, @Nullable Logger logger);

    void startScreenRecorder(@NotNull File folder, int maxTimeInSecond, @Nullable Logger logger);

    void stopScreenRecorder();

    String startLogCollector(@NotNull String pkgName, @NotNull TestRun testRun, @NotNull Logger logger);

    void stopLogCollector();

    void testDeviceSetup(@Nullable Logger logger) throws IOException;

    void testDeviceUnset(Logger logger);

    void setRunningTestName(String runningTestName);

    String getName();

    String getSerialNum();

    String getOsVersion();

    void killAll();

    void addCurrentTask(TestTask testTask);

    void finishTask();

    void quitMobileAppiumDriver(Logger logger);

    void updateScreenshotImageAsyncDelay(long delayMillis, @NotNull FileAvailableCallback fileAvailableCallback, @NotNull Logger logger);

    void grantAllTaskNeededPermissions(@NotNull TestTask testTask, @Nullable Logger logger);

    void runAppiumMonkey(String packageName, int round, Logger logger);

    boolean runAppiumT2CTest(File jsonFile, Logger reportLogger);

    interface FileAvailableCallback {
        void onFileReady(File file);
    }
}