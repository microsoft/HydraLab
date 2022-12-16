// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.management;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.common.util.IOSUtils;
import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.common.util.ThreadUtils;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.IOSMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.appium.java_client.windows.WindowsDriver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.net.UrlChecker;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


@Service
public class AppiumServerManager {
    public static final String EDGE_DRIVER_DOWNLOAD_URL = "https://msedgedriver.azureedge.net/";
    public static final String EDGE_DRIVER_ZIP = "edgedriver_win64.zip";
    public static final String EDGE_DRIVER_EXE = "msedgedriver.exe";
    public static final String EDGE_DRIVER_VERSION_TXT = "msedgedriverversion.txt";
    public static final String EDGE_PROCESS_NAME = "msedge";
    public static final String WINDOWS_HANDLE_BY_APP_FAMILY_ID_SCRIPT_NAME = "WindowsAppIdToHandle.ps1";
    private final Map<String, IOSDriver> iOSDrivers = new ConcurrentHashMap<>();
    private final Map<String, AndroidDriver> androidDrivers = new ConcurrentHashMap<>();
    private final Map<String, WindowsDriver> windowsAppDrivers = new ConcurrentHashMap<>();
    private AppiumDriverLocalService service;
    private int appiumServerPort = 10086;
    private String appiumServerHost = "127.0.0.1";
    private String workspacePath;
    private String edgeDriverZipPath;
    private String edgeDriverName;
    private String edgeDriverVersionFile;
    private WindowsDriver windowsRootDriver;
    private EdgeDriver edgeDriver;
    private WindowsDriver winEdgeDriver;

    public AppiumServerManager() {
        setWorkspacePath("./");
    }

    public void startAppiumServer() {
        System.out.println("get appium service ");
        if (service == null || !service.isRunning()) {
            try {
                URL status = new URL(String.format("http://%s:%d/wd/hub/status", appiumServerHost, appiumServerPort));
                new UrlChecker().waitUntilAvailable(Duration.ofMillis(1500).toMillis(), TimeUnit.MILLISECONDS, status);
                System.out.println("Appium service is running");
            } catch (UrlChecker.TimeoutException | MalformedURLException e) {
                service = AppiumDriverLocalService.buildService(
                        new AppiumServiceBuilder()
                                .usingPort(appiumServerPort)
                                .withArgument(GeneralServerFlag.BASEPATH, "/wd/hub/")
                                .withArgument(GeneralServerFlag.RELAXED_SECURITY)
                                .withArgument(GeneralServerFlag.LOG_LEVEL, "error")
                                .withArgument(GeneralServerFlag.ALLOW_INSECURE, "adb_shell")
                );
                service.start();
                System.out.println("start appium service : " + service.isRunning());
            }
        }
    }

    public void stopAppiumServer() {
        if (service != null) {
            service.stop();
            service = null;
        }
    }

    public void setAppiumServerHost(String appiumServerHost) {
        this.appiumServerHost = appiumServerHost;
    }

    public void setAppiumServerPort(int appiumServerPort) {
        this.appiumServerPort = appiumServerPort;
    }

    public IOSDriver getIOSDriver(DeviceInfo deviceInfo, Logger logger) {
        startAppiumServer();

        String udid = deviceInfo.getSerialNum();
        IOSDriver iosDriver = iOSDrivers.get(udid);
        if (iosDriver != null && isDriverAlive(iosDriver)) {
            logger.info(iosDriver.toString());
            logger.info(iosDriver.getStatus().toString());
            return iosDriver;
        }

        int wdaPort = IOSUtils.getWdaPortByUdid(udid, logger);
        DesiredCapabilities caps = new DesiredCapabilities();

        caps.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 4000);
        caps.setCapability(MobileCapabilityType.PLATFORM_NAME, "iOS");
        caps.setCapability(MobileCapabilityType.AUTOMATION_NAME, "XCUITest");
        caps.setCapability(IOSMobileCapabilityType.XCODE_SIGNING_ID, "iPhone Developer");

        caps.setCapability(MobileCapabilityType.PLATFORM_VERSION, deviceInfo.getOsVersion());
        caps.setCapability(MobileCapabilityType.DEVICE_NAME, deviceInfo.getName());
        caps.setCapability(MobileCapabilityType.UDID, udid);

        caps.setCapability(IOSMobileCapabilityType.WEB_DRIVER_AGENT_URL, "http://127.0.0.1:" + wdaPort);
        caps.setCapability(IOSMobileCapabilityType.WDA_LOCAL_PORT, wdaPort);
        caps.setCapability(IOSMobileCapabilityType.USE_PREBUILT_WDA, false);
        caps.setCapability("useXctestrunFile", false);
        caps.setCapability("skipLogCapture", true);
        caps.setCapability("mjpegServerPort", IOSUtils.getMjpegServerPortByUdid(udid, logger, deviceInfo));

        int tryTimes = 3;
        boolean sessionCreated = false;
        while (tryTimes > 0 && !sessionCreated) {
            if (iosDriver != null) {
                iosDriver.quit();
            }
            tryTimes--;
            try {
                iosDriver = new IOSDriver(new URL(String.format("http://%s:%d/wd/hub", appiumServerHost, appiumServerPort)), caps);

                // Check the session is created, if not, try to create a new one.
                URL status = new URL(String.format("http://127.0.0.1:%d/wd/hub/session/%s", appiumServerPort, iosDriver.getSessionId()));
                new UrlChecker().waitUntilAvailable(Duration.ofMillis(1500).toMillis(), TimeUnit.MILLISECONDS, status);

                logger.info("Create Driver, SessionID: " + iosDriver.getSessionId());
                iOSDrivers.put(udid, iosDriver);
                sessionCreated = true;
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (UrlChecker.TimeoutException e) {
                logger.info("SessionID: " + iosDriver.getSessionId() + " doesn't exist in WDA");
            }
        }
        return iosDriver;
    }

    public AndroidDriver getAndroidDriver(DeviceInfo deviceInfo, Logger logger) {
        startAppiumServer();

        String udid = deviceInfo.getSerialNum();
        AndroidDriver androidDriver = androidDrivers.get(udid);
        if (androidDriver != null && isDriverAlive(androidDriver)) {
            return androidDriver;
        }

        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 4000);
        caps.setCapability(MobileCapabilityType.UDID, udid);

        try {
            androidDriver = new AndroidDriver(new URL(String.format("http://%s:%d/wd/hub", appiumServerHost, appiumServerPort)), caps);
            androidDrivers.put(udid, androidDriver);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return androidDriver;
    }

    public WindowsDriver getWindowsAppDriver(String appFamilyName, Logger logger) {
        startAppiumServer();

        WindowsDriver windowsAppDriver = windowsAppDrivers.get(appFamilyName);
        if (windowsAppDriver != null && isDriverAlive(windowsAppDriver)) {
            return windowsAppDriver;
        }

        DesiredCapabilities caps;

        // Launch Windows App
        ShellUtils.execLocalCommand(ShellUtils.POWER_SHELL_PATH + " -Command " + "start-process shell:AppsFolder\\" + appFamilyName + "!App", logger);
        ThreadUtils.safeSleep(5000);
        int tries = 3;
        for (int i = 0; i < tries; i++) {
            caps = new DesiredCapabilities();
            String hexAppTopLevelWindowByKeyWord = getHexAppTopLevelWindowByFamilyName(appFamilyName, logger);
            if (hexAppTopLevelWindowByKeyWord.length() == 0) {
                throw new RuntimeException(appFamilyName + " is not opened or failed activate it");
            }
            System.out.println("HexAppTopLevelWindowByKeyWord: " + hexAppTopLevelWindowByKeyWord);
            caps.setCapability("platformName", "Windows");
            caps.setCapability("deviceName", "WindowsPC");
            caps.setCapability("appTopLevelWindow", hexAppTopLevelWindowByKeyWord);
            caps.setCapability("newCommandTimeout", 4000);
            try {
                windowsAppDriver = new WindowsDriver(new URL(String.format("http://%s:%d/wd/hub", appiumServerHost, appiumServerPort)), caps);
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            } catch (NoSuchWindowException e) {
                if (i == tries - 1) {
                    throw e;
                }
            }
        }
        windowsAppDrivers.put(appFamilyName, windowsAppDriver);

        return windowsAppDriver;
    }

    @Nonnull
    private String getHexAppTopLevelWindowByFamilyName(String appFamilyName, Logger logger) {
        String name = WINDOWS_HANDLE_BY_APP_FAMILY_ID_SCRIPT_NAME;
        File scriptFile = new File(name);
        if (!scriptFile.exists()) {
            try (InputStream resourceAsStream = FileUtils.class.getClassLoader().getResourceAsStream(name); OutputStream out = new FileOutputStream(scriptFile)) {
                IOUtils.copy(Objects.requireNonNull(resourceAsStream), out);
            } catch (IOException e) {
                logger.error("Fail to find app handler script", e);
            }
        }
        String processInfo = ShellUtils.execLocalCommandWithResult(ShellUtils.POWER_SHELL_PATH + " -Command " + scriptFile.getAbsolutePath() + " " + appFamilyName, logger);
        if (processInfo != null && processInfo.length() > 0) {
            logger.info("handlerIdStr: " + processInfo);
            int handlerIdInt = Integer.parseInt(processInfo);
            return Integer.toHexString(handlerIdInt);
        } else {
            return "";
        }
    }

    @Nonnull
    private String getHexAppTopLevelWindowByProcessName(String processName, Logger logger) {
        String processInfo = ShellUtils.execLocalCommandWithResult(ShellUtils.POWER_SHELL_PATH + " -Command " + "\"(Get-Process | where {$_.mainWindowTitle -and $_.mainWindowHandle -ne 0 -and $_.Name -eq '" + processName + "'} | Select mainWindowHandle).mainWindowHandle\"", logger);
        logger.info(processName + " processInfo: " + processInfo);
        if (processInfo != null && processInfo.length() > 0) {
            String handlerIdStr = processInfo.trim().split(" ")[0];
            logger.info(processName + " handlerIdStr: " + handlerIdStr);
            int handlerIdInt = Integer.parseInt(handlerIdStr);
            return Integer.toHexString(handlerIdInt);
        } else {
            return "";
        }
    }

    public WindowsDriver getWindowsRootDriver(Logger logger) {
        startAppiumServer();

        if (windowsRootDriver == null || !isDriverAlive(windowsRootDriver)) {
            DesiredCapabilities caps = new DesiredCapabilities();
            caps.setCapability("platformName", "Windows");
            caps.setCapability("app", "Root");
            caps.setCapability("deviceName", "WindowsPC");
            caps.setCapability("newCommandTimeout", 4000);
            try {
                windowsRootDriver = new WindowsDriver(new URL(String.format("http://%s:%d/wd/hub", appiumServerHost, appiumServerPort)), caps);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        return windowsRootDriver;
    }

    public Boolean isDriverAlive(AppiumDriver driver) {
        try {
            driver.getScreenshotAs(OutputType.FILE);
            return true;
        } catch (NoSuchSessionException e) {
            return false;
        }
    }

    public void setWorkspacePath(String path) {
        workspacePath = path;
        edgeDriverZipPath = new File(workspacePath, EDGE_DRIVER_ZIP).getAbsolutePath();
        edgeDriverName = new File(workspacePath, EDGE_DRIVER_EXE).getAbsolutePath();
        edgeDriverVersionFile = new File(workspacePath, EDGE_DRIVER_VERSION_TXT).getAbsolutePath();
    }


    public WindowsDriver getWindowsEdgeDriver(Logger logger) {
        startAppiumServer();

        if (winEdgeDriver == null || !isDriverAlive(winEdgeDriver)) {
            String hexAppTopLevelWindow = getHexAppTopLevelWindowByProcessName(EDGE_PROCESS_NAME, logger);
            logger.info("Edge hexAppTopLevelWindow: " + hexAppTopLevelWindow);
            DesiredCapabilities caps = new DesiredCapabilities();
            caps.setCapability("platformName", "Windows");
            caps.setCapability("deviceName", "WindowsPC");
            caps.setCapability("appTopLevelWindow", hexAppTopLevelWindow);
            caps.setCapability("newCommandTimeout", 4000);
            try {
                winEdgeDriver = new WindowsDriver(new URL(String.format("http://%s:%d/wd/hub", appiumServerHost, appiumServerPort)), caps);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        return winEdgeDriver;
    }

    public EdgeDriver getEdgeDriver(Logger logger) {
        startAppiumServer();

        if (edgeDriver == null) {
            File edgeDriverFile = new File(edgeDriverName);
            if (!edgeDriverFile.exists()) {
                installLatestEdgeDriver();
            }
            System.setProperty("webdriver.edge.driver", edgeDriverName);

            try {
                edgeDriver = new EdgeDriver();
            } catch (Exception e) {
                e.printStackTrace();
                updateEdgeDriver(e.getMessage());
                edgeDriver = new EdgeDriver();
            }
        }

        return edgeDriver;
    }

    public void installLatestEdgeDriver() {
        try {
            FileUtil.downloadFileUsingStream(EDGE_DRIVER_DOWNLOAD_URL + "/LATEST_STABLE", edgeDriverVersionFile);
            String version = FileUtil.getStringFromFilePath(edgeDriverVersionFile);
            version = version.replaceAll("[^\\d.]", "");
            System.out.println(version);
            downloadEdgeDriver(version);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateEdgeDriver(String errorMessage) {
//        forceKillEdgeDriver();
        int startIndex = errorMessage.indexOf("version is ");
        int endIndex = errorMessage.indexOf(" with binary path");
        if (startIndex < endIndex) {
            String[] msgArr = errorMessage.substring(startIndex, endIndex).split(" ");
            if (msgArr.length == 3) {
                String version = msgArr[2];
                downloadEdgeDriver(version);
            }
        }
    }

    private void downloadEdgeDriver(String version) {
        try {
            FileUtil.downloadFileUsingStream(EDGE_DRIVER_DOWNLOAD_URL + version + "/" + EDGE_DRIVER_ZIP, edgeDriverZipPath);
            FileUtil.unzipFile(edgeDriverZipPath, workspacePath);
            // wait 2s to ensure the un-zip finished
            ThreadUtils.safeSleep(TimeUnit.SECONDS.toMillis(2));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void forceKillEdgeDriver() {
        ProcessBuilder builder = new ProcessBuilder("taskkill", "/F", "/im", "msedgedriver64.exe");
        try {
            builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // wait 2s to ensure that process is totally crashed
        ThreadUtils.safeSleep(TimeUnit.SECONDS.toMillis(2));

    }


    public void quitIOSDriver(DeviceInfo deviceInfo, Logger logger) {
        String udid = deviceInfo.getSerialNum();
        logger.info("Quiting the driver for device: " + udid);
        IOSUtils.releaseMjpegServerPortByUdid(udid, logger);
        IOSDriver iosDriver = iOSDrivers.get(udid);
        if (iosDriver != null) {
            try {
                iosDriver.quit();
                logger.info("Quited the driver for device: " + udid);
            } catch (Exception e) {
                logger.info("Error happened when quiting driver for device: " + udid);
                e.printStackTrace();
            }
        }
        iOSDrivers.remove(udid);

    }

    public void quitAndroidDriver(DeviceInfo deviceInfo, Logger logger) {
        String udid = deviceInfo.getSerialNum();
        logger.info("Quiting the driver for device: " + udid);
        AndroidDriver androidDriver = androidDrivers.get(udid);
        if (androidDriver != null) {
            try {
                androidDriver.quit();
                logger.info("Quited the driver for device: " + udid);
            } catch (Exception e) {
                logger.info("Error happened when quiting driver for device: " + udid);
                e.printStackTrace();
            }
        }
        androidDrivers.remove(udid);
    }

    public void quitWindowsRootDriver(Logger logger) {
        if (windowsRootDriver != null) {
            try {
                windowsRootDriver.quit();
                windowsRootDriver = null;
                logger.info("Quited the driver for Windows. ");
            } catch (Exception e) {
                logger.info("Error happened when quiting driver for Windows. ");
                e.printStackTrace();
            }
        }
    }

    public void quitWindowsAppDriver(String appFamilyName, Logger logger) {

        logger.info("Quiting the Windows driver for app: " + appFamilyName);
        WindowsDriver windowsDriver = windowsAppDrivers.get(appFamilyName);
        if (windowsDriver != null) {
            try {
                windowsDriver.quit();
                logger.info("Quited the Windows driver for app: " + appFamilyName);
            } catch (Exception e) {
                logger.info("Error happened when quiting Windows driver for app: " + appFamilyName);
                e.printStackTrace();
            }
        }
        windowsAppDrivers.remove(appFamilyName);
    }

    public void quitEdgeDriver(Logger logger) {
        if (edgeDriver != null) {
            try {
                edgeDriver.quit();
                edgeDriver = null;
                logger.info("Quited the driver for edge.");
            } catch (Exception e) {
                logger.info("Error happened when quiting driver for edge.");
                e.printStackTrace();
            }
        }
    }

    public void quitWindowsEdgeDriver(Logger logger) {
        if (winEdgeDriver != null) {
            try {
                winEdgeDriver.quit();
                winEdgeDriver = null;
                logger.info("Quite the driver for win edge.");
            } catch (Exception e) {
                logger.info("Error happened when quiting driver for windows edge.");
                e.printStackTrace();
            }
        }
    }
}
