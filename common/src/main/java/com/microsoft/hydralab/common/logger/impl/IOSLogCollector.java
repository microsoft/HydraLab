// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.logger.impl;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.management.impl.IOSDeviceManager;
import com.microsoft.hydralab.common.util.IOSUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class IOSLogCollector implements LogCollector {
    private final DeviceInfo connectedDevice;
    private final TestRun testRun;
    private final String pkgName;
    private final Logger infoLogger;
    IOSDeviceManager deviceManager;
    private boolean started;
    private String loggerFilePath;
    private Process logProcess;
    private boolean crashFound;

    public IOSLogCollector(DeviceManager deviceManager, DeviceInfo deviceInfo, String pkgName, TestRun testRun, Logger logger) {
        this.deviceManager = (IOSDeviceManager) deviceManager;
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
        loggerFilePath = new File(testRun.getResultFolder(), "iOSSysLog.log").getAbsolutePath();
        try {
            // Clear the crash happened before UI test start.
            IOSUtils.collectCrashInfo(testRun.getResultFolder() + "/LegacyCrash", connectedDevice, infoLogger);
            logProcess = IOSUtils.startIOSLog(pkgName, loggerFilePath, connectedDevice, infoLogger);
            if (logProcess != null) {
                connectedDevice.addCurrentProcess(logProcess);
            }
            infoLogger.info("Start to fetch the system log of iOS device");
        }catch (Exception e) {
            infoLogger.info("fail to fetch the system log of iOS device");
        }
        return loggerFilePath;
    }

    @Override
    public void stopAndAnalyse() {
        started = false;
        if (logProcess != null && logProcess.isAlive()) {
            logProcess.destroy();
        }
        try {
            // Collect the crash logs
            String crashFilesPath = testRun.getResultFolder() + "/Crash";
            IOSUtils.collectCrashInfo(crashFilesPath, connectedDevice, infoLogger);
            File dir = new File(crashFilesPath);
            StringBuilder crashLines = new StringBuilder();
            boolean firstCrash = true;
            for (File f : Objects.requireNonNull(dir.listFiles())) {
                if (!f.isDirectory() && f.getName().endsWith(".ips")) {
                    if (firstCrash) {
                        firstCrash = false;
                    } else {
                        crashLines.append("----------------------------------Next------------------------------------------").append("\n");
                    }
                    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            crashLines.append(line).append("\n");
                        }
                    }
                }
            }
            if (crashLines.length() > 0) {
                crashFound = true;
                testRun.setCrashStack(crashLines.toString());
                testRun.setCrashStackId(UUID.randomUUID().toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (logProcess != null) {
                logProcess.destroy();
            }
        }
    }

    @Override
    public boolean isCrashFound() {
        return crashFound;
    }

}
