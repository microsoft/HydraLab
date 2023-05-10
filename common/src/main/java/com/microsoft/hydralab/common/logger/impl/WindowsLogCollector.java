// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.logger.impl;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.logger.LogCollector;
import org.slf4j.Logger;

import java.io.File;

public class WindowsLogCollector implements LogCollector {
    private final DeviceInfo connectedDevice;
    private final TestRun testRun;
    private final String pkgName;
    private final Logger infoLogger;
    private String loggerFilePath;
    private boolean started;

    public WindowsLogCollector(DeviceInfo deviceInfo, String pkgName, TestRun testRun, Logger logger) {
        this.connectedDevice = deviceInfo;
        this.testRun = testRun;
        this.pkgName = pkgName;
        this.infoLogger = logger;
    }

    @Override
    public String start() {
        if (started) {
            return loggerFilePath;
        }
        started = true;
        loggerFilePath = new File(testRun.getResultFolder(), "win-logcat.log").getAbsolutePath();
        return loggerFilePath;
    }

    @Override
    public void stopAndAnalyse() {

    }

    @Override
    public boolean isCrashFound() {
        return false;
    }
}
