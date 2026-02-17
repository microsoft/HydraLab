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
    public static final String WDA_BUNDLE_ID = "com.microsoft.WebDriverAgentRunner.xctrunner";
    private static final Map<String, Integer> wdaPortMap = new ConcurrentHashMap<>();
    private static final Map<String, Integer> mjpegServerPortMap = new ConcurrentHashMap<>();
    private static final Set<Integer> PORT_BLACK_LIST = new HashSet<>() {{
        add(8080);  //Reserved port
        add(8100);  //for WDA
        add(9100);  //For ffmpeg
        add(10086); //For appium
    }};

    public static void collectCrashInfo(String folder, DeviceInfo deviceInfo, Logger logger) {
        ShellUtils.execLocalCommand("pymobiledevice3 crash pull " + folder + " --udid " + deviceInfo.getSerialNum(), logger);
    }

    @Nullable
    public static Process startIOSLog(String keyWord, String logFilePath, DeviceInfo deviceInfo, Logger logger) {
        Process logProcess = null;
        File logFile = new File(logFilePath);
        logProcess = ShellUtils.execLocalCommandWithRedirect("pymobiledevice3 syslog live -m " + keyWord + " --udid " + deviceInfo.getSerialNum(), logFile, false, logger);
        return logProcess;
    }

    public static void startIOSDeviceWatcher(Logger logger, IOSDeviceDriver deviceDriver) {
        Process process = null;
        String command = "pymobiledevice3 remote tunneld";
        ShellUtils.killProcessByCommandStr(command, logger);
        try {
            process = Runtime.getRuntime().exec(command);
            IOSDeviceWatcher err = new IOSDeviceWatcher(process.getErrorStream(), logger, deviceDriver);
            IOSDeviceWatcher out = new IOSDeviceWatcher(process.getInputStream(), logger, deviceDriver);
            err.start();
            out.start();
            logger.info("Successfully run: " + command);
        } catch (Exception e) {
            throw new HydraLabRuntimeException("Failed to run: " + command, e);
        }
    }

    @Nullable
    public static String getIOSDeviceListJsonStr(Logger logger) {
        return ShellUtils.execLocalCommandWithResult("t3 list --json", logger);
    }

    @Nullable
    public static String getAppList(String udid, Logger logger) {
        return ShellUtils.execLocalCommandWithResult("t3 -u " + udid + " app list", logger);
    }

    public static void installApp(String udid, String packagePath, Logger logger) {
        ShellUtils.execLocalCommand(String.format("t3 -u %s install \"%s\"", udid, packagePath.replace(" ", "\\ ")), logger);
    }

    @Nullable
    public static String uninstallApp(String udid, String packageName, Logger logger) {
        return ShellUtils.execLocalCommandWithResult("t3 -u " + udid + "app uninstall " + packageName, logger);
    }

    public static void launchApp(String udid, String packageName, Logger logger) {
        ShellUtils.execLocalCommand("t3 -u " + udid + "app launch " + packageName, logger);
    }

    public static void stopApp(String udid, String packageName, Logger logger) {
        ShellUtils.execLocalCommand("t3 -u " + udid + "app kill " + packageName, logger);
    }

    public static void proxyWDA(DeviceInfo deviceInfo, Logger logger) {
        String udid = deviceInfo.getSerialNum();
        int wdaPort = getWdaPortByUdid(udid, logger);
        if (isWdaRunningByPort(wdaPort, logger)) {
            return;
        }
        // String command = "tidevice -u " + udid + " wdaproxy -B " + WDA_BUNDLE_ID + " --port " + getWdaPortByUdid(udid, logger);
        String portRelayCommand = "t3 -u " + udid + " relay " + wdaPort + " 8100";
        String startWDACommand = "pymobiledevice3 developer dvt launch " + WDA_BUNDLE_ID + " --udid " + udid;

        deviceInfo.addCurrentProcess(ShellUtils.execLocalCommand(startWDACommand, false, logger));
        deviceInfo.addCurrentProcess(ShellUtils.execLocalCommand(portRelayCommand, false, logger));
        if (!isWdaRunningByPort(wdaPort, logger)) {
            logger.error("Agent may not proxy WDA correctly. Port {} is not accessible", wdaPort);
        }
    }

    public static void killProxyWDA(DeviceInfo deviceInfo, Logger logger) {
        String udid = deviceInfo.getSerialNum();
        int wdaPort = getWdaPortByUdid(udid, logger);
        // String command = "tidevice -u " + udid + " wdaproxy -B " + WDA_BUNDLE_ID + " --port " + getWdaPortByUdid(udid, logger);
        // We can still try to kill the process even the proxy is not running.
        String portRelayCommand = "t3 -u " + udid + " relay " + wdaPort + " 8100";
        String startWDACommand = "t3 -u " + udid + "  xctest --bundle_id " + WDA_BUNDLE_ID;

        ShellUtils.killProcessByCommandStr(portRelayCommand, logger);
        ShellUtils.killProcessByCommandStr(startWDACommand, logger);
    }

    @Nullable
    public static String getIOSDeviceDetailInfo(String udid, Logger logger) {
        return ShellUtils.execLocalCommandWithResult("t3 -u " + udid + " info", logger);
    }

    public static void takeScreenshot(String udid, String screenshotFilePath, Logger logger) {
        ShellUtils.execLocalCommand("pymobiledevice3 developer dvt screenshot \"" + screenshotFilePath + "\"" + " --udid " + udid, logger);
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
            Process process = ShellUtils.execLocalCommand("t3 -u " + serialNum + " relay " + mjpegServerPor + " 9100", false, classLogger);
            ThreadUtils.safeSleep(2000);
            deviceInfo.addCurrentProcess(process);
            mjpegServerPortMap.put(serialNum, mjpegServerPor);
        }
        classLogger.info("get mjpeg port = " + mjpegServerPortMap.get(serialNum));
        return mjpegServerPortMap.get(serialNum);
    }

    public static void releaseMjpegServerPortByUdid(String serialNum, Logger classLogger) {
        if (mjpegServerPortMap.containsKey(serialNum)) {
            int mjpegServerPor = mjpegServerPortMap.get(serialNum);
            ShellUtils.killProcessByCommandStr("t3 -u " + serialNum + " relay " + mjpegServerPor + " 9100", classLogger);
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
