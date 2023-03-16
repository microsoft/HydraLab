package com.microsoft.hydralab.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IOSPerfTestHelper {
    private static IOSPerfTestHelper instance;

    private Logger classLogger = LoggerFactory.getLogger(getClass());

    private static final String lowestVersion = "0.10.2";
    private static final String installCommand = "pip3 install -U tidevice[openssl]";
    private Map<String, IOSPerfScriptProcessInfo> iOSPerfScriptProcesses;

    private IOSPerfTestHelper() {
        iOSPerfScriptProcesses = new ConcurrentHashMap<>();

        // Check version of tidevice
        boolean needUpdate = false;
        String versionString = ShellUtils.execLocalCommandWithResult("tidevice -v", classLogger);
        if (versionString == null) {
            needUpdate = true;
        } else {
            String[] currentVersionParts = versionString.trim().split("\\.");
            String[] lowestVersionParts = versionString.trim().split("\\.");
            for (int i = 0; i < currentVersionParts.length; i++) {
                int currentPart = Integer.parseInt(currentVersionParts[i]);
                int requiredPart = Integer.parseInt(lowestVersionParts[i]);
                if (currentPart < requiredPart) {
                    needUpdate = true;
                    break;
                }
            }
        }
        if (needUpdate) {
            ShellUtils.execLocalCommand(installCommand, classLogger);
        }
    }

    public void add(String key, File resultFile, Process process) {
        if (!isRunning(key)) {
            iOSPerfScriptProcesses.put(key, new IOSPerfScriptProcessInfo(resultFile, process));
        }
    }

    public boolean isRunning(String key) {
        return iOSPerfScriptProcesses.containsKey(key);
    }

    @Nullable
    public File getResultFile(String key) {
        IOSPerfScriptProcessInfo processInfo = iOSPerfScriptProcesses.get(key);
        if (processInfo != null) {
            File outputFile = processInfo.getResultFile();
            return outputFile;
        }
        return null;
    }

    public long getStartTime(String key) {
        IOSPerfScriptProcessInfo processInfo = iOSPerfScriptProcesses.get(key);
        if (processInfo != null) {
            return processInfo.getStartTimestamp();
        }
        return 0;
    }

    public void stop(String key) {
        IOSPerfScriptProcessInfo processInfo = iOSPerfScriptProcesses.get(key);
        if (processInfo != null) {
            processInfo.stopProcess();
            iOSPerfScriptProcesses.remove(key);
        }
    }

    public static IOSPerfTestHelper getInstance() {
        if (instance == null) {
            instance = new IOSPerfTestHelper();
        }
        return instance;
    }

    private static class IOSPerfScriptProcessInfo {
        private File resultFile;
        private Process process;

        private long startTimestamp;

        IOSPerfScriptProcessInfo(File resultFile, Process process) {
            this.resultFile = resultFile;
            this.process = process;
            this.startTimestamp = System.currentTimeMillis();
        }

        public long getStartTimestamp() {
            return startTimestamp;
        }

        public File getResultFile() {
            return resultFile;
        }

        public void stopProcess() {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
