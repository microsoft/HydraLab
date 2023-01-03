// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.logger.impl;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.LogUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ADBLogcatCollector implements LogCollector {
    private final DeviceInfo connectedDevice;
    private final TestRun deviceTestResult;
    private final String pkgName;
    private final Logger infoLogger;
    DeviceManager deviceManager;
    ADBOperateUtil adbOperateUtil;
    private boolean started;
    private String loggerFilePath;

    public ADBLogcatCollector(DeviceManager deviceManager, ADBOperateUtil adbOperateUtil, DeviceInfo deviceInfo, String pkgName, TestRun deviceTestResult, Logger logger) {
        this.deviceManager = deviceManager;
        this.adbOperateUtil = adbOperateUtil;
        this.connectedDevice = deviceInfo;
        this.deviceTestResult = deviceTestResult;
        this.pkgName = pkgName;
        this.infoLogger = logger;
    }

    @Override
    public String start() {
        if (started) {
            return loggerFilePath;
        }
        started = true;
        loggerFilePath = new File(deviceTestResult.getTestRunResultFolder(), "logcat.log").getAbsolutePath();
        runCommand("logcat -G 48M");
        runCommand("logcat -c");
        return loggerFilePath;
    }

    private void runCommand(String comm) {
        Process process = null;
        try {
            process = adbOperateUtil.executeDeviceCommandOnPC(connectedDevice, comm, infoLogger);
            process.waitFor(3, TimeUnit.SECONDS);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    @Override
    public void stopAndAnalyse() {
        started = false;
        Logger logger = LogUtils.getLoggerWithRollingFileAppender(
                DeviceManager.LOGGER_PREFIX + "logcat_" + connectedDevice.getSerialNum(),
                loggerFilePath,
                "%logger{0}>> %m%n");
        Process process = null;
        try {
            // AlarmManager:S System:S
            process = adbOperateUtil.executeDeviceCommandOnPC(connectedDevice, "logcat -d Finsky:S MirrorLink:S *:D", infoLogger);

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                boolean collectCrash = false;
                StringBuilder crashLines = new StringBuilder();
                while ((line = bufferedReader.readLine()) != null) {
                    if (collectCrash) {
                        if (!line.contains(" E ") && !line.contains(" F ")) {
                            collectCrash = false;
                        } else {
                            if (line.contains(pkgName)) {
                                crashLines.append("<b>").append(line).append("</b>").append("\n");
                            } else {
                                crashLines.append(line).append("\n");
                            }
                        }
                    }
                    if (line.contains("beginning of crash") || line.contains("AndroidRuntime: FATAL EXCEPTION")) {
                        collectCrash = true;
                    }
                    logger.info(line);
                }
                if (crashLines.length() > 0) {
                    deviceTestResult.setCrashStack(crashLines.toString());
                    deviceTestResult.setCrashStackId(UUID.randomUUID().toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
            LogUtils.releaseLogger(logger);
        }
    }

    @Override
    public boolean isCrashFound() {
        return StringUtils.isNotEmpty(deviceTestResult.getCrashStack());
    }
}
