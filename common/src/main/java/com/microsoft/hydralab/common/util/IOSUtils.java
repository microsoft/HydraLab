// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.device.impl.IOSTestDeviceManager;
import org.openqa.selenium.net.UrlChecker;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
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
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"tidevice", "-u", deviceInfo.getSerialNum(), "crashreport ", folder});
            process.waitFor();
        } catch (InterruptedException | IOException e) {
            logger.error("Ignored Error: ", e);
        }
    }

    @Nullable
    public static Process startIOSLog(String keyWord, String logFilePath, DeviceInfo deviceInfo, Logger logger) {
        Process logProcess = null;
        File logFile = new File(logFilePath);
        if (ShellUtils.isConnectedToWindowsOS) {
            logProcess = ShellUtils.execLocalCommandWithRedirect("tidevice -u " + deviceInfo.getSerialNum() + " syslog | findstr /i \"" + keyWord + "\"", logFile, false, logger);
        } else {
            logProcess = ShellUtils.execLocalCommandWithRedirect("tidevice -u " + deviceInfo.getSerialNum() + " syslog | grep -i \"" + keyWord + "\"", logFile, false, logger);
        }
        return logProcess;
    }

    public static void startIOSDeviceWatcher(Logger logger, IOSTestDeviceManager deviceManager) {
        Process process = null;
        String command = "tidevice watch";
        ShellUtils.killProcessByCommandStr(command, logger);
        try {
            process = Runtime.getRuntime().exec(command);
            IOSDeviceWatcher err = new IOSDeviceWatcher(process.getErrorStream(), logger, deviceManager);
            IOSDeviceWatcher out = new IOSDeviceWatcher(process.getInputStream(), logger, deviceManager);
            err.start();
            out.start();
            logger.info("Successfully run: " + command);
        } catch (Exception e) {
            logger.error("Fail to run: " + command, e);
        }
    }

    @Nullable
    public static String getIOSDeviceListJsonStr(Logger logger) {
        return ShellUtils.execLocalCommandWithResult("tidevice list --json", logger);
    }

    @Nullable
    public static String getAppList(String udid, Logger logger) {
        return ShellUtils.execLocalCommandWithResult("tidevice -u " + udid + " applist", logger);
    }

    public static void installApp(String udid, String packagePath, Logger logger) {
        ShellUtils.execLocalCommand(String.format("tidevice -u %s install %s", udid, packagePath.replace(" ", "\\ ")), logger);
    }

    @Nullable
    public static String uninstallApp(String udid, String packageName, Logger logger) {
        return ShellUtils.execLocalCommandWithResult("tidevice -u " + udid + " uninstall " + packageName, logger);
    }

    public static void launchApp(String udid, String packageName, Logger logger) {
        ShellUtils.execLocalCommand("tidevice -u " + udid + " launch " + packageName, logger);
    }

    public static void stopApp(String udid, String packageName, Logger logger) {
        ShellUtils.execLocalCommand("tidevice -u " + udid + " kill " + packageName, logger);
    }

    public static void proxyWDA(DeviceInfo deviceInfo, Logger logger) {
        String udid = deviceInfo.getSerialNum();
        int wdaPort = getWdaPortByUdid(udid, logger);
        // String command = "tidevice -u " + udid + " wdaproxy -B " + WDA_BUNDLE_ID + " --port " + getWdaPortByUdid(udid, logger);
        String portRelayCommand = "tidevice -u " + udid + " relay " + wdaPort + " 8100";
        String startWDACommand = "tidevice -u " + udid + "  xctest --bundle_id " + WDA_BUNDLE_ID;

        deviceInfo.addCurrentProcess(ShellUtils.execLocalCommand(portRelayCommand, false, logger));
        deviceInfo.addCurrentProcess(ShellUtils.execLocalCommand(startWDACommand, false, logger));
        if (!isWdaRunningByPort(wdaPort, logger)) {
            logger.error("Agent may not proxy WDA correctly. Port {} is not accessible", wdaPort);
        }
    }

    public static void killProxyWDA(DeviceInfo deviceInfo, Logger logger) {
        String udid = deviceInfo.getSerialNum();
        int wdaPort = getWdaPortByUdid(udid, logger);
        // String command = "tidevice -u " + udid + " wdaproxy -B " + WDA_BUNDLE_ID + " --port " + getWdaPortByUdid(udid, logger);
        String portRelayCommand = "tidevice -u " + udid + " relay " + wdaPort + " 8100";
        String startWDACommand = "tidevice -u " + udid + "  xctest --bundle_id " + WDA_BUNDLE_ID;

        ShellUtils.killProcessByCommandStr(portRelayCommand, logger);
        ShellUtils.killProcessByCommandStr(startWDACommand, logger);
    }

    @Nullable
    public static String getIOSDeviceDetailInfo(String udid, Logger logger) {
        return ShellUtils.execLocalCommandWithResult("tidevice -u " + udid + " info --json", logger);
    }

    public static void takeScreenshot(String udid, String screenshotFilePath, Logger logger) {
        ShellUtils.execLocalCommand("tidevice -u " + udid + " screenshot " + screenshotFilePath, logger);
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

    public static int getMjpegServerPortByUdid(String serialNum, Logger classLogger, DeviceInfo deviceInfo) {
        if (!mjpegServerPortMap.containsKey(serialNum) || !isPortOccupied(mjpegServerPortMap.get(serialNum), classLogger)) {
            // Randomly assign a port
            int mjpegServerPor = generateRandomPort(classLogger);
            classLogger.info("Generate a new mjpeg port = " + mjpegServerPor);
            Process process = ShellUtils.execLocalCommand("tidevice -u " + serialNum + " relay " + mjpegServerPor + " 9100", false, classLogger);
            deviceInfo.addCurrentProcess(process);
            mjpegServerPortMap.put(serialNum, mjpegServerPor);
        }
        classLogger.info("get mjpeg port = " + mjpegServerPortMap.get(serialNum));
        return mjpegServerPortMap.get(serialNum);
    }

    public static void releaseMjpegServerPortByUdid(String serialNum, Logger classLogger) {
        if (mjpegServerPortMap.containsKey(serialNum)) {
            int mjpegServerPor = mjpegServerPortMap.get(serialNum);
            ShellUtils.killProcessByCommandStr("tidevice -u " + serialNum + " relay " + mjpegServerPor + " 9100", classLogger);
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
        String result;
        result = ShellUtils.execLocalCommandWithResult("netstat -ant", classLogger);
        boolean b = result != null && result.contains(Integer.toString(port));
        classLogger.info("isPortOccupied: " + port + "  " + b);
        return b;
    }
}
