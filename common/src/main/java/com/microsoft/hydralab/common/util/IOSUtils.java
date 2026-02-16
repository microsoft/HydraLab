// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.device.impl.IOSDeviceDriver;
import org.openqa.selenium.net.UrlChecker;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class IOSUtils {
    public static final String WDA_BUNDLE_ID = "com.microsoft.wdar.xctrunner.xctrunner";
    private static final Map<String, Integer> wdaPortMap = new ConcurrentHashMap<>();
    private static final Map<String, Integer> mjpegServerPortMap = new ConcurrentHashMap<>();
    private static final Set<Integer> PORT_BLACK_LIST = new HashSet<>() {{
        add(8080);  //Reserved port
        add(8100);  //for WDA
        add(9100);  //For ffmpeg
        add(10086); //For appium
    }};

    public static void collectCrashInfo(String folder, DeviceInfo deviceInfo, Logger logger) {
        ShellUtils.execLocalCommand("python3 -m pymobiledevice3 crash pull --udid " + deviceInfo.getSerialNum() + " " + folder, logger);
    }

    @Nullable
    public static Process startIOSLog(String keyWord, String logFilePath, DeviceInfo deviceInfo, Logger logger) {
        Process logProcess = null;
        File logFile = new File(logFilePath);
        if (ShellUtils.isConnectedToWindowsOS) {
            logProcess = ShellUtils.execLocalCommandWithRedirect("python3 -m pymobiledevice3 syslog live --udid " + deviceInfo.getSerialNum() + " | findstr /i \"" + keyWord + "\"", logFile, false, logger);
        } else {
            logProcess = ShellUtils.execLocalCommandWithRedirect("python3 -m pymobiledevice3 syslog live --udid " + deviceInfo.getSerialNum() + " | grep -i \"" + keyWord + "\"", logFile, false, logger);
        }
        return logProcess;
    }

    public static void startIOSDeviceWatcher(Logger logger, IOSDeviceDriver deviceDriver) {
        // Note: pymobiledevice3 does not have 'usbmux watch' command
        // Device monitoring is now handled through periodic polling in updateAllDeviceInfo()
        logger.info("iOS device watcher initialized - using polling mechanism instead of continuous watch");
        // Trigger initial device discovery
        deviceDriver.updateAllDeviceInfo();
    }

    @Nullable
    public static String getIOSDeviceListJsonStr(Logger logger) {
        return ShellUtils.execLocalCommandWithResult("python3 -m pymobiledevice3 usbmux list", logger);
    }

    @Nullable
    public static String getAppList(String udid, Logger logger) {
        return ShellUtils.execLocalCommandWithResult("python3 -m pymobiledevice3 apps list --udid " + udid, logger);
    }

    public static void installApp(String udid, String packagePath, Logger logger) {
        ShellUtils.execLocalCommand(String.format("python3 -m pymobiledevice3 apps install --udid %s \"%s\"", udid, packagePath.replace(" ", "\\ ")), logger);
    }

    @Nullable
    public static String uninstallApp(String udid, String packageName, Logger logger) {
        return ShellUtils.execLocalCommandWithResult("python3 -m pymobiledevice3 apps uninstall --udid " + udid + " " + packageName, logger);
    }

    public static void launchApp(String udid, String packageName, Logger logger) {
        ShellUtils.execLocalCommand("python3 -m pymobiledevice3 developer dvt launch --udid " + udid + " " + packageName, logger);
    }

    public static void stopApp(String udid, String packageName, Logger logger) {
        // Note: pymobiledevice3 kill requires PID, not bundle ID
        // Workaround: Launch with --kill-existing flag to terminate existing instance
        ShellUtils.execLocalCommand("python3 -m pymobiledevice3 developer dvt launch --udid " + udid + " --kill-existing " + packageName, logger);
        logger.warn("stopApp() using launch with --kill-existing workaround. App will be relaunched then immediately stopped.");
    }

    public static void proxyWDA(DeviceInfo deviceInfo, Logger logger) {
        String udid = deviceInfo.getSerialNum();
        int wdaPort = getWdaPortByUdid(udid, logger);
        if (isWdaRunningByPort(wdaPort, logger)) {
            return;
        }
        // Note: usbmux forward uses --serial, not --udid
        String portRelayCommand = "python3 -m pymobiledevice3 usbmux forward --serial " + udid + " " + wdaPort + " 8100";
        String startWDACommand = "python3 -m pymobiledevice3 developer dvt launch --udid " + udid + " " + WDA_BUNDLE_ID;

        deviceInfo.addCurrentProcess(ShellUtils.execLocalCommand(portRelayCommand, false, logger));
        deviceInfo.addCurrentProcess(ShellUtils.execLocalCommand(startWDACommand, false, logger));
        if (!isWdaRunningByPort(wdaPort, logger)) {
            logger.error("Agent may not proxy WDA correctly. Port {} is not accessible", wdaPort);
        }
    }

    public static void killProxyWDA(DeviceInfo deviceInfo, Logger logger) {
        String udid = deviceInfo.getSerialNum();
        int wdaPort = getWdaPortByUdid(udid, logger);
        // Note: usbmux forward uses --serial, not --udid
        // We can still try to kill the process even the proxy is not running.
        String portRelayCommand = "python3 -m pymobiledevice3 usbmux forward --serial " + udid + " " + wdaPort + " 8100";
        String startWDACommand = "python3 -m pymobiledevice3 developer dvt launch --udid " + udid + " " + WDA_BUNDLE_ID;

        ShellUtils.killProcessByCommandStr(portRelayCommand, logger);
        ShellUtils.killProcessByCommandStr(startWDACommand, logger);
    }

    @Nullable
    public static String getIOSDeviceDetailInfo(String udid, Logger logger) {
        return ShellUtils.execLocalCommandWithResult("python3 -m pymobiledevice3 lockdown info --udid " + udid, logger);
    }

    public static void takeScreenshot(String udid, String screenshotFilePath, Logger logger) {
        ShellUtils.execLocalCommand("python3 -m pymobiledevice3 developer dvt screenshot --udid " + udid + " \"" + screenshotFilePath + "\"", logger);
    }

    public static boolean isWdaRunningByPort(int port, Logger logger) {
        try {
            URL status = new URL("http://127.0.0.1:" + port + "/status");
            new UrlChecker().waitUntilAvailable(Duration.ofMillis(20000).toMillis(), TimeUnit.MILLISECONDS, status);
            logger.info("WDA proxy is running on {}.", port);
            return true;
        } catch (Exception e) {
            logger.error("No WDA proxy is running on {}.", port);
            return false;
        }
    }

    public static int getWdaPortByUdid(String serialNum, Logger classLogger) {
        if (!wdaPortMap.containsKey(serialNum)) {
            // Randomly assign a port
            int wdaPort = generateRandomPort(classLogger);
            wdaPortMap.put(serialNum, wdaPort);
        }
        classLogger.info("get Wda port = " + wdaPortMap.get(serialNum));
        return wdaPortMap.get(serialNum);
    }

    /**
     * Gets or reserves an MJPEG server port for the device WITHOUT setting up forwarding.
     * Use this when Appium will handle its own port forwarding (e.g., on Mac).
     * For Windows where we need manual ffmpeg-based recording, use getMjpegServerPortByUdid instead.
     */
    public static int reserveMjpegServerPortByUdid(String serialNum, Logger classLogger) {
        if (mjpegServerPortMap.containsKey(serialNum)) {
            int cachedPort = mjpegServerPortMap.get(serialNum);
            // For reserved ports (no forwarding), we just check if the port is still free
            if (!isPortOccupied(cachedPort, classLogger)) {
                classLogger.info("Reusing reserved mjpeg port = " + cachedPort);
                return cachedPort;
            } else {
                // Port got occupied by something else, need a new one
                classLogger.warn("Reserved mjpeg port " + cachedPort + " is now occupied, generating new");
                mjpegServerPortMap.remove(serialNum);
            }
        }
        
        // Generate a new port but DON'T set up forwarding - let Appium handle it
        int mjpegServerPort = generateRandomPort(classLogger);
        classLogger.info("Reserved new mjpeg port = " + mjpegServerPort + " (no forwarding - Appium will handle)");
        mjpegServerPortMap.put(serialNum, mjpegServerPort);
        return mjpegServerPort;
    }

    /**
     * Gets an MJPEG server port and sets up pymobiledevice3 forwarding.
     * Use this for Mac/Windows where we manually record with ffmpeg.
     */
    public static int getMjpegServerPortByUdid(String serialNum, Logger classLogger, DeviceInfo deviceInfo) {
        // Check if we have a cached port and if it's still active
        if (mjpegServerPortMap.containsKey(serialNum)) {
            int cachedPort = mjpegServerPortMap.get(serialNum);
            if (isPortOccupied(cachedPort, classLogger)) {
                classLogger.info("Reusing existing mjpeg port = " + cachedPort);
                return cachedPort;
            } else {
                // Port is no longer occupied, clean up and create new
                classLogger.warn("Cached mjpeg port " + cachedPort + " is no longer active, cleaning up");
                releaseMjpegServerPortByUdid(serialNum, classLogger);
            }
        }
        
        // Generate a new port and set up forwarding
        int mjpegServerPort = generateRandomPort(classLogger);
        classLogger.info("Generate a new mjpeg port = " + mjpegServerPort);
        // Note: usbmux forward uses --serial, not --udid
        Process process = ShellUtils.execLocalCommand("python3 -m pymobiledevice3 usbmux forward --serial " + serialNum + " " + mjpegServerPort + " 9100", false, classLogger);
        deviceInfo.addCurrentProcess(process);
        mjpegServerPortMap.put(serialNum, mjpegServerPort);
        
        // Wait for the port forwarding to become active (up to 10 seconds)
        classLogger.info("Waiting for MJPEG port {} to become active...", mjpegServerPort);
        boolean portReady = waitForPortToBeListening(mjpegServerPort, 10000, classLogger);
        if (!portReady) {
            classLogger.warn("MJPEG port {} may not be ready, but continuing anyway", mjpegServerPort);
        } else {
            classLogger.info("MJPEG port {} is now active", mjpegServerPort);
        }
        
        return mjpegServerPort;
    }
    
    /**
     * Waits for a port to start listening.
     * @param port The port to check
     * @param timeoutMs Maximum time to wait in milliseconds
     * @param logger Logger for debug output
     * @return true if port is listening, false if timeout
     */
    private static boolean waitForPortToBeListening(int port, int timeoutMs, Logger logger) {
        long startTime = System.currentTimeMillis();
        int checkInterval = 500; // Check every 500ms
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            String result = ShellUtils.execLocalCommandWithResult("lsof -i :" + port + " -t", logger);
            if (result != null && !result.trim().isEmpty()) {
                return true;
            }
            try {
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public static void releaseMjpegServerPortByUdid(String serialNum, Logger classLogger) {
        if (mjpegServerPortMap.containsKey(serialNum)) {
            int mjpegServerPor = mjpegServerPortMap.get(serialNum);
            // Note: usbmux forward uses --serial, not --udid
            ShellUtils.killProcessByCommandStr("python3 -m pymobiledevice3 usbmux forward --serial " + serialNum + " " + mjpegServerPor + " 9100", classLogger);
            mjpegServerPortMap.remove(serialNum, mjpegServerPor);
        }
    }

    private static int generateRandomPort(Logger classLogger) {
        Random random = new Random();
        int port;
        do {
            // 7000 - 9999
            port = random.nextInt(10000 - 7000) + 7000;
        } while (wdaPortMap.containsValue(port) || PORT_BLACK_LIST.contains(port) || isPortOccupied(port, classLogger));
        return port;
    }

    private static boolean isPortOccupied(int port, Logger classLogger) {
        // Use lsof which is more reliable for checking port usage, including forwarded ports
        String result = ShellUtils.execLocalCommandWithResult("lsof -i :" + port + " -t", classLogger);
        boolean occupied = result != null && !result.trim().isEmpty();
        
        // Also check if pymobiledevice3 is forwarding this port (process-based check)
        String forwardCheck = ShellUtils.execLocalCommandWithResult(
            "ps aux | grep 'pymobiledevice3 usbmux forward' | grep ' " + port + " ' | grep -v grep", 
            classLogger
        );
        boolean forwardActive = forwardCheck != null && !forwardCheck.trim().isEmpty();
        
        boolean isOccupied = occupied || forwardActive;
        classLogger.info("isPortOccupied: " + port + " (lsof: " + occupied + ", forward: " + forwardActive + ") = " + isOccupied);
        return isOccupied;
    }
}
