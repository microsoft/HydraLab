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
    public static final String WDA_BUNDLE_ID = "com.microsoft.wdar.xctrunner";
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
        // --tunnel UDID required for iOS 17+ devices when tunneld is running
        ShellUtils.execLocalCommand("python3 -m pymobiledevice3 developer dvt launch --tunnel " + udid + " " + packageName, logger);
    }

    public static void stopApp(String udid, String packageName, Logger logger) {
        // Note: pymobiledevice3 kill requires PID, not bundle ID
        // Workaround: Launch with --kill-existing flag to terminate existing instance
        // --tunnel UDID required for iOS 17+ devices when tunneld is running
        ShellUtils.execLocalCommand("python3 -m pymobiledevice3 developer dvt launch --tunnel " + udid + " --kill-existing " + packageName, logger);
        logger.warn("stopApp() using launch with --kill-existing workaround. App will be relaunched then immediately stopped.");
    }

    private static final int WDA_MAX_RETRIES = 3;
    private static final int WDA_INITIAL_WAIT_MS = 3000;
    private static final int WDA_READY_CHECK_TIMEOUT_MS = 10000;

    /**
     * Start WDA proxy for iOS device.
     * This method:
     * 1. Starts port forwarding from local port to device port 8100
     * 2. Launches WDA on the device
     * 3. Waits for WDA to be ready with retry logic
     *
     * @param deviceInfo The iOS device info
     * @param logger Logger instance
     */
    public static void proxyWDA(DeviceInfo deviceInfo, Logger logger) {
        String udid = deviceInfo.getSerialNum();
        int wdaPort = getWdaPortByUdid(udid, logger);

        // Check if WDA is already running
        if (isWdaRunningByPortQuick(wdaPort, logger)) {
            logger.info("✅ WDA already running on port {}", wdaPort);
            return;
        }

        logger.info("🚀 Starting WDA proxy for device {} on port {}...", udid, wdaPort);

        // Kill any existing WDA processes for this device
        killProxyWDA(deviceInfo, logger);

        // Step 1: Start port forwarding FIRST
        // Try iproxy first (more reliable), fallback to pymobiledevice3
        Process portForwardProcess = startPortForwarding(udid, wdaPort, 8100, logger);
        deviceInfo.addCurrentProcess(portForwardProcess);

        // Wait for port forwarding to establish
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Step 2: Launch WDA with retries
        String startWDACommand = "python3 -m pymobiledevice3 developer dvt launch --tunnel " + udid + " " + WDA_BUNDLE_ID;
        boolean wdaStarted = false;

        for (int attempt = 1; attempt <= WDA_MAX_RETRIES && !wdaStarted; attempt++) {
            logger.info("🎯 WDA launch attempt {}/{}", attempt, WDA_MAX_RETRIES);

            // Launch WDA
            Process wdaProcess = ShellUtils.execLocalCommand(startWDACommand, false, logger);
            deviceInfo.addCurrentProcess(wdaProcess);

            // Wait for WDA to initialize (increases with each retry)
            int waitTime = WDA_INITIAL_WAIT_MS * attempt;
            logger.info("⏳ Waiting {}ms for WDA to initialize...", waitTime);
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            // Check if WDA is ready
            wdaStarted = isWdaRunningByPortWithTimeout(wdaPort, WDA_READY_CHECK_TIMEOUT_MS, logger);

            if (!wdaStarted && attempt < WDA_MAX_RETRIES) {
                logger.warn("⚠️ WDA not ready after attempt {}. Retrying...", attempt);
                // Kill WDA process before retry
                ShellUtils.killProcessByCommandStr(startWDACommand, logger);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (wdaStarted) {
            logger.info("✅ WDA proxy started successfully on port {}", wdaPort);
        } else {
            logger.error("❌ Failed to start WDA after {} attempts. Port {} is not accessible.", WDA_MAX_RETRIES, wdaPort);
            logger.error("💡 Ensure: 1) tunneld is running (sudo python3 -m pymobiledevice3 remote tunneld)");
            logger.error("💡         2) WDA is installed with bundle ID: {}", WDA_BUNDLE_ID);
            logger.error("💡         3) Developer certificate is trusted on device");
        }
    }

    /**
     * Start port forwarding from localhost to device.
     * Tries iproxy first (more reliable), falls back to pymobiledevice3.
     *
     * @param udid Device UDID
     * @param localPort Local port to listen on
     * @param devicePort Port on the device
     * @param logger Logger instance
     * @return The port forwarding process
     */
    private static Process startPortForwarding(String udid, int localPort, int devicePort, Logger logger) {
        // Check if iproxy is available (more reliable for iOS)
        String iproxyCheck = ShellUtils.execLocalCommandWithResult("which iproxy", logger);
        if (iproxyCheck != null && !iproxyCheck.isEmpty() && !iproxyCheck.contains("not found")) {
            logger.info("📡 Using iproxy for port forwarding: localhost:{} -> device:{}", localPort, devicePort);
            String iproxyCommand = "iproxy " + localPort + " " + devicePort + " -u " + udid;
            return ShellUtils.execLocalCommand(iproxyCommand, false, logger);
        } else {
            logger.info("📡 Using pymobiledevice3 for port forwarding: localhost:{} -> device:{}", localPort, devicePort);
            String portRelayCommand = "python3 -m pymobiledevice3 usbmux forward --serial " + udid + " " + localPort + " " + devicePort;
            return ShellUtils.execLocalCommand(portRelayCommand, false, logger);
        }
    }

    /**
     * Quick check if WDA is running (short timeout).
     */
    private static boolean isWdaRunningByPortQuick(int port, Logger logger) {
        return isWdaRunningByPortWithTimeout(port, 3000, logger);
    }

    /**
     * Check if WDA is running with configurable timeout.
     */
    private static boolean isWdaRunningByPortWithTimeout(int port, int timeoutMs, Logger logger) {
        try {
            URL status = new URL("http://127.0.0.1:" + port + "/status");
            new UrlChecker().waitUntilAvailable(timeoutMs, TimeUnit.MILLISECONDS, status);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Kill WDA proxy processes for a device.
     * Cleans up both port forwarding and WDA launch processes.
     *
     * @param deviceInfo The iOS device info
     * @param logger Logger instance
     */
    public static void killProxyWDA(DeviceInfo deviceInfo, Logger logger) {
        String udid = deviceInfo.getSerialNum();
        int wdaPort = getWdaPortByUdid(udid, logger);

        logger.info("🚭 Killing WDA proxy processes for device {}...", udid);

        // Kill port forwarding processes (both iproxy and pymobiledevice3)
        String iproxyCommand = "iproxy " + wdaPort + " 8100 -u " + udid;
        String portRelayCommand = "python3 -m pymobiledevice3 usbmux forward --serial " + udid + " " + wdaPort + " 8100";
        ShellUtils.killProcessByCommandStr(iproxyCommand, logger);
        ShellUtils.killProcessByCommandStr(portRelayCommand, logger);

        // Kill WDA launch process
        String startWDACommand = "python3 -m pymobiledevice3 developer dvt launch --tunnel " + udid + " " + WDA_BUNDLE_ID;
        ShellUtils.killProcessByCommandStr(startWDACommand, logger);

        // Also kill any iproxy process on the port
        ShellUtils.killProcessByCommandStr("iproxy " + wdaPort, logger);
    }

    @Nullable
    public static String getIOSDeviceDetailInfo(String udid, Logger logger) {
        return ShellUtils.execLocalCommandWithResult("python3 -m pymobiledevice3 lockdown info --udid " + udid, logger);
    }

    /**
     * Take a screenshot of the iOS device.
     * @param udid Device UDID
     * @param screenshotFilePath Path to save the screenshot
     * @param logger Logger instance
     * @return true if screenshot was successfully created, false otherwise
     */
    public static boolean takeScreenshot(String udid, String screenshotFilePath, Logger logger) {
        // --tunnel UDID required for iOS 17+ devices when tunneld is running
        ShellUtils.execLocalCommand("python3 -m pymobiledevice3 developer dvt screenshot --tunnel " + udid + " \"" + screenshotFilePath + "\"", logger);
        // Verify screenshot was created
        File screenshot = new File(screenshotFilePath);
        if (!screenshot.exists() || screenshot.length() == 0) {
            logger.error("❌ Screenshot file was not created or is empty: {}. Ensure tunneld is running: sudo python3 -m pymobiledevice3 remote tunneld", screenshotFilePath);
            return false;
        }
        logger.info("✅ Screenshot captured successfully: {} ({}KB)", screenshotFilePath, screenshot.length() / 1024);
        return true;
    }

    /**
     * Check if WDA is running on the specified port.
     * Uses a 20-second timeout for the check.
     *
     * @param port The port to check
     * @param logger Logger instance
     * @return true if WDA is responding on the port
     */
    public static boolean isWdaRunningByPort(int port, Logger logger) {
        try {
            URL status = new URL("http://127.0.0.1:" + port + "/status");
            new UrlChecker().waitUntilAvailable(Duration.ofMillis(20000).toMillis(), TimeUnit.MILLISECONDS, status);
            logger.info("✅ WDA proxy is running on port {}.", port);
            return true;
        } catch (Exception e) {
            logger.warn("⚠️ No WDA proxy detected on port {}.", port);
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
     * Get or create MJPEG server port for device.
     * NOTE: We do NOT start iproxy here - Appium handles MJPEG port forwarding internally.
     * Starting iproxy here would conflict with Appium's own port forwarding.
     *
     * @param serialNum Device serial number
     * @param classLogger Logger instance
     * @param deviceInfo Device info to track processes (unused, kept for API compatibility)
     * @return The MJPEG server port
     */
    public static int getMjpegServerPortByUdid(String serialNum, Logger classLogger, DeviceInfo deviceInfo) {
        if (!mjpegServerPortMap.containsKey(serialNum) || isPortOccupied(mjpegServerPortMap.get(serialNum), classLogger)) {
            // Generate a random free port for MJPEG
            // Appium will handle the actual port forwarding to the device
            int mjpegServerPort = generateRandomPort(classLogger);
            classLogger.info("🎥 Allocated MJPEG port {} for device {} (Appium will handle forwarding)", mjpegServerPort, serialNum);
            mjpegServerPortMap.put(serialNum, mjpegServerPort);
        }
        classLogger.info("get mjpeg port = " + mjpegServerPortMap.get(serialNum));
        return mjpegServerPortMap.get(serialNum);
    }

    /**
     * Release MJPEG server port for device.
     * Kills both iproxy and pymobiledevice3 port forwarding processes.
     *
     * @param serialNum Device serial number
     * @param classLogger Logger instance
     */
    public static void releaseMjpegServerPortByUdid(String serialNum, Logger classLogger) {
        if (mjpegServerPortMap.containsKey(serialNum)) {
            int mjpegServerPort = mjpegServerPortMap.get(serialNum);
            classLogger.info("🚭 Releasing MJPEG port {} for device {}", mjpegServerPort, serialNum);

            // Kill both possible port forwarders
            ShellUtils.killProcessByCommandStr("iproxy " + mjpegServerPort + " 9100 -u " + serialNum, classLogger);
            ShellUtils.killProcessByCommandStr("python3 -m pymobiledevice3 usbmux forward --serial " + serialNum + " " + mjpegServerPort + " 9100", classLogger);
            ShellUtils.killProcessByCommandStr("iproxy " + mjpegServerPort, classLogger);

            mjpegServerPortMap.remove(serialNum, mjpegServerPort);
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
        String result;
        result = ShellUtils.execLocalCommandWithResult("netstat -ant", classLogger);
        boolean b = result != null && result.contains(Integer.toString(port));
        classLogger.info("isPortOccupied: " + port + "  " + b);
        return b;
    }
}
