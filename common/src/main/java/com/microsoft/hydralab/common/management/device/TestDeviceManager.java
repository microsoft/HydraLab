// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.management.device;

import com.android.ddmlib.InstallException;
import com.android.ddmlib.TimeoutException;
import com.microsoft.hydralab.agent.runner.ITestRun;
import com.microsoft.hydralab.agent.runner.TestRunThreadContext;
import com.microsoft.hydralab.common.entity.common.*;
import com.microsoft.hydralab.common.logger.LogCollector;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.AppiumServerManager;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.*;
import io.appium.java_client.appmanagement.ApplicationState;
import io.appium.java_client.ios.IOSDriver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.android.ddmlib.IDevice.DeviceState;


public abstract class TestDeviceManager {
    static final Logger classLogger = LoggerFactory.getLogger(TestDeviceManager.class);
    protected final AgentManagementService agentManagementService;
    protected AppiumServerManager appiumServerManager;

    public TestDeviceManager(AgentManagementService agentManagementService, AppiumServerManager appiumServerManager) {
        this.agentManagementService = agentManagementService;
        this.appiumServerManager = appiumServerManager;
    }

    public abstract void init();

    public static MobileDeviceState mobileDeviceStateMapping(DeviceState adbState) {
        if (adbState == null) {
            return MobileDeviceState.OTHER;
        }

        switch (adbState) {
            case ONLINE:
                return MobileDeviceState.ONLINE;
            case OFFLINE:
                return MobileDeviceState.OFFLINE;
            case DISCONNECTED:
                return MobileDeviceState.DISCONNECTED;
            default:
                return MobileDeviceState.OTHER;
        }
    }

    public AppiumServerManager getAppiumServerManager() {
        return appiumServerManager;
    }

    public abstract File getScreenShot(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) throws Exception;

    public File getScreenShotWithStrategy(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger, @NotNull AgentUser.BatteryStrategy batteryStrategy) throws Exception {
        File screenshotImageFile = deviceInfo.getScreenshotImageFile();
        if (screenshotImageFile == null || StringUtils.isEmpty(deviceInfo.getScreenshotImageUrl())) {
            return getScreenShot(deviceInfo, logger);
        } else if (batteryStrategy.wakeUpInterval > 0) {
            synchronized (deviceInfo.getLock()) {
                long now = System.currentTimeMillis();
                if (now - deviceInfo.getScreenshotUpdateTimeMilli() < TimeUnit.SECONDS.toMillis(batteryStrategy.wakeUpInterval)) {
                    classLogger.warn("skip screen shot for too short interval {}", deviceInfo.getName());
                    return screenshotImageFile;
                }
                getScreenShot(deviceInfo, logger);
            }
        }
        return screenshotImageFile;
    }

    public abstract void wakeUpDevice(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger);

    public abstract void backToHome(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger);

    public abstract void grantPermission(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull String permissionName, @Nullable Logger logger);

    public abstract void addToBatteryWhiteList(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull Logger logger);

    public abstract boolean installApp(@NotNull DeviceInfo deviceInfo, @NotNull String packagePath, @Nullable Logger logger) throws InstallException;

    public abstract boolean uninstallApp(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger) throws InstallException;

    public abstract void resetPackage(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger);

    public abstract void pushFileToDevice(@NotNull DeviceInfo deviceInfo, @NotNull String pathOnAgent, @NotNull String pathOnDevice, @Nullable Logger logger) throws Exception;

    public abstract void pullFileFromDevice(@NotNull DeviceInfo deviceInfo, @NotNull String pathOnDevice, @Nullable Logger logger) throws Exception;

    public abstract ScreenRecorder getScreenRecorder(@NotNull DeviceInfo deviceInfo, @NotNull File folder, @Nullable Logger logger);

    public boolean grantAllTaskNeededPermissions(@NotNull DeviceInfo deviceInfo, @NotNull TestTask task, @Nullable Logger logger) {
        return false;
    }

    public boolean grantAllPackageNeededPermissions(@NotNull DeviceInfo deviceInfo, @NotNull File packageFile, @NotNull String targetPackage, boolean allowCustomizedPermissions, @Nullable Logger logger) {
        return false;
    }

    public Logger getDeviceLogger(DeviceInfo device) {
        String file = agentManagementService.getDeviceLogBaseDir().getAbsolutePath() + "/" + device.getName() + "/device_control.log";
        return LogUtils.getLoggerWithRollingFileAppender(LogCollector.LOGGER_PREFIX + device.getSerialNum(), file, "%d %logger{0} %p [%t] - %m%n");
    }


    public void updateScreenshotImageAsyncDelay(@NotNull DeviceInfo deviceInfo, long delayMillis, @NotNull FileAvailableCallback fileAvailableCallback, @NotNull Logger logger) {
        ThreadPoolUtil.SCREENSHOT_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ThreadUtils.safeSleep(delayMillis);
                    File imageFile = getScreenShot(deviceInfo, logger);
                    if (fileAvailableCallback != null) {
                        fileAvailableCallback.onFileReady(imageFile);
                    }
                } catch (TimeoutException te) {
                    classLogger.error("{}: {}, updateScreenshotImageAsyncDelay", te.getClass().getSimpleName(), te.getMessage());
                } catch (Exception e) {
                    classLogger.error(e.getMessage(), e);
                }
            }
        });
    }

    public void updateAllDeviceInfo() {
    }

    public abstract LogCollector getLogCollector(@NotNull DeviceInfo deviceInfo, @NotNull String pkgName, @NotNull TestRun testRun, @NotNull Logger logger);

    public abstract void setProperty(@NotNull DeviceInfo deviceInfo, @NotNull String property, String val, @Nullable Logger logger);

    public abstract boolean setDefaultLauncher(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @NotNull String defaultActivity, @Nullable Logger logger);

    public abstract boolean isAppInstalled(@NotNull DeviceInfo deviceInfo, @NotNull String packageName, @Nullable Logger logger);

    public abstract boolean grantProjectionAndBatteryPermission(@NotNull DeviceInfo deviceInfo, @NotNull String recordPackageName, @Nullable Logger logger);

    public abstract void testDeviceSetup(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) throws IOException;

    public abstract void removeFileInDevice(DeviceInfo deviceInfo, String pathOnDevice, Logger logger);

    public abstract void testDeviceUnset(DeviceInfo deviceInfo, Logger logger);

    abstract public WebDriver getMobileAppiumDriver(DeviceInfo deviceInfo, Logger logger);

    abstract public void quitMobileAppiumDriver(DeviceInfo deviceInfo, Logger logger);

    abstract public void execCommandOnDevice(DeviceInfo deviceInfo, String command, Logger logger);

    public void execCommandOnAgent(DeviceInfo deviceInfo, String command, Logger logger) {
        ITestRun testRun = TestRunThreadContext.getTestRun();
        String newCommand = command;
        if (testRun != null) {
            // Variable only supported when the test run is ready
            Assert.notNull(testRun.getResultFolder(), "The testRun instance in ThreadContext does not have resultFolder property!");
            newCommand = ShellUtils.parseHydraLabVariable(command, testRun, deviceInfo);
        }
        ShellUtils.execLocalCommand(newCommand, logger);
    }

    protected boolean isAppRunningForeground(DeviceInfo deviceInfo, String packageName, Logger logger) {
        IOSDriver iOSDriver = appiumServerManager.getIOSDriver(deviceInfo, logger);
        ApplicationState state = iOSDriver.queryAppState(packageName);
        boolean result = (state == ApplicationState.RUNNING_IN_FOREGROUND);
        if (!result) {
            logger.info("State of App " + packageName + " is: " + state.toString());
        }
        return result;
    }

    public void runAppiumMonkey(DeviceInfo deviceInfo, String packageName, int round, Logger logger) {
        try {
            for (int i = 0; i < round; i++) {
                logger.info("Monkey Test Round " + i + "[Started]");
                WebDriver driver = getMobileAppiumDriver(deviceInfo, logger);
                // Select all the leaf nodes
                List<WebElement> eleList = driver.findElements(By.xpath("//*[not(*)]"));
                int count = eleList.size();
                logger.info("Found " + count + " element(s)");
                if (StringUtils.isEmpty(packageName)) {
                    if (count == 0) {
                        logger.info("No element Found, Back to Home Screen.");
                        backToHome(deviceInfo, logger);
                        eleList = driver.findElements(By.xpath("//*"));
                        count = eleList.size();
                    }
                } else {
                    if (count == 0 || !isAppRunningForeground(deviceInfo, packageName, logger)) {
                        logger.info("No element Found or App is Running in Background, Back to Home Screen and Restart App.");
                        backToHome(deviceInfo, logger);
                        IOSUtils.launchApp(deviceInfo.getSerialNum(), packageName, logger);
                        eleList = driver.findElements(By.xpath("//*"));
                        count = eleList.size();
                    }
                }
                if (count <= 0) {
                    continue;
                }
                int r = new Random().nextInt(count);
                WebElement e = eleList.get(r);
                try {
                    String name = e.getText();
                    logger.info("Select NO. " + r + " element: " + name);
                    e.click();
                } catch (StaleElementReferenceException | ElementNotInteractableException ignore) {
                    // Cached element is not exist any more skip this try.
                }
                logger.info("Monkey Test Round " + i + "[Done]");
            }
        } catch (WebDriverException e) {
            e.printStackTrace();
            logger.info("Monkey Test Exit with Error, Quit the Driver. ");
            quitMobileAppiumDriver(deviceInfo, logger);
        }
    }

    public boolean runAppiumT2CTest(DeviceInfo deviceInfo, File jsonFile, Logger reportLogger) {
        return true;
    }

    public interface FileAvailableCallback {
        void onFileReady(File file);
    }
}
