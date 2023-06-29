// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.management.device.impl;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.lang.Assert;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.RawImage;
import com.microsoft.hydralab.agent.runner.ITestRun;
import com.microsoft.hydralab.agent.runner.TestRunThreadContext;
import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.logger.MultiLineNoCancelLoggingReceiver;
import com.microsoft.hydralab.common.logger.MultiLineNoCancelReceiver;
import com.microsoft.hydralab.common.logger.impl.ADBLogcatCollector;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.AppiumServerManager;
import com.microsoft.hydralab.common.management.device.DeviceType;
import com.microsoft.hydralab.common.screen.PhoneAppScreenRecorder;
import com.microsoft.hydralab.common.screen.ScreenRecorder;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.common.util.ThreadUtils;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.android.ddmlib.IDevice.CHANGE_BUILD_INFO;
import static com.android.ddmlib.IDevice.CHANGE_CLIENT_LIST;
import static com.android.ddmlib.IDevice.CHANGE_STATE;
import static com.android.ddmlib.IDevice.DeviceState;
import static com.android.ddmlib.IDevice.PROP_BUILD_API_LEVEL;
import static com.android.ddmlib.IDevice.PROP_BUILD_VERSION;
import static com.android.ddmlib.IDevice.PROP_DEVICE_CPU_ABI_LIST;
import static com.android.ddmlib.IDevice.PROP_DEVICE_MANUFACTURER;
import static com.android.ddmlib.IDevice.PROP_DEVICE_MODEL;
import static com.microsoft.hydralab.common.screen.PhoneAppScreenRecorder.recordPackageName;

public class AndroidDeviceDriver extends AbstractDeviceDriver {

    public static final int HOME_EVENT = 3;
    public static final String KEYCODE_WAKEUP = "KEYCODE_WAKEUP";
    public static final String KEYCODE_MENU = "KEYCODE_MENU";
    public static final String KEYCODE_BACK = "KEYCODE_BACK";
    public static final String KEYCODE_HOME = "KEYCODE_HOME";
    static final Logger classLogger = LoggerFactory.getLogger(AndroidDeviceDriver.class);
    private final Map<String, DeviceInfo> adbDeviceInfoMap = new HashMap<>();
    private static final int MAJOR_ADB_VERSION = 1;
    private static final int MINOR_ADB_VERSION = -1;
    ADBOperateUtil adbOperateUtil;

    private final AndroidDebugBridge.IDeviceChangeListener mListener =
            new AndroidDebugBridge.IDeviceChangeListener() {
                @Override
                public void deviceConnected(IDevice device) {
                    // from disconnected to connected (OFFLINE)
                    classLogger.warn("DeviceConnected {} {} {}", device.getSerialNumber(), device.getState(),
                            device.getName());
                    deviceInfoUpdate(device);
                    DeviceInfo deviceInfo = adbDeviceInfoMap.get(device.getSerialNumber());
                    if (deviceInfo == null) {
                        return;
                    }
                    if (device.getState().equals(DeviceState.ONLINE)) {
                        agentManagementService.getDeviceStatusListenerManager().onDeviceConnected(deviceInfo);
                    } else {
                        agentManagementService.getDeviceStatusListenerManager().onDeviceInactive(deviceInfo);
                    }
                }

                @Override
                public void deviceDisconnected(IDevice device) {
                    classLogger.error("DeviceDisconnected {} {} {}", device.getSerialNumber(), device.getState(),
                            device.getName());
                    deviceInfoUpdate(device);
                    DeviceInfo deviceInfo = adbDeviceInfoMap.get(device.getSerialNumber());
                    if (deviceInfo == null) {
                        return;
                    }

                    agentManagementService.getDeviceStatusListenerManager().onDeviceInactive(deviceInfo);
                    appiumServerManager.quitAndroidDriver(deviceInfo, classLogger);
                }

                @Override
                public void deviceChanged(IDevice device, int changeMask) {
                    if (changeMask != CHANGE_STATE) {
                        String changeType = "";
                        if (changeMask == CHANGE_BUILD_INFO) {
                            changeType = "build info";
                        } else if (changeMask == CHANGE_CLIENT_LIST) {
                            changeType = "client list";
                        }
                        classLogger.warn("DeviceChanged, SN: {}, name: {}, {} changed", device.getSerialNumber(),
                                device.getName(), changeType);
                        return;
                    }

                    classLogger.warn("DeviceChanged {} {} {}", device.getSerialNumber(), device.getState(),
                            device.getName());
                    deviceInfoUpdate(device);
                    DeviceInfo deviceInfo = adbDeviceInfoMap.get(device.getSerialNumber());
                    if (deviceInfo == null) {
                        return;
                    }

                    if (device.getState().equals(DeviceState.ONLINE)) {
                        agentManagementService.getDeviceStatusListenerManager().onDeviceConnected(deviceInfo);
                    } else {
                        agentManagementService.getDeviceStatusListenerManager().onDeviceInactive(deviceInfo);
                    }
                }
            };

    private static boolean isAndroidCommonPermission(String usesPermission) {
        return usesPermission.startsWith("android.");
    }

    public AndroidDeviceDriver(AgentManagementService agentManagementService,
                               AppiumServerManager appiumServerManager, ADBOperateUtil adbOperateUtil) {
        super(agentManagementService, appiumServerManager);
        this.adbOperateUtil = adbOperateUtil;
    }

    @Override
    public void init() {
        try {
            adbOperateUtil.init(mListener);
            PhoneAppScreenRecorder.copyAPK(agentManagementService.getPreAppDir());
        } catch (Exception e) {
            throw new HydraLabRuntimeException(500, "adbOperateUtil init failed", e);
        }
    }

    @Override
    public List<EnvCapabilityRequirement> getEnvCapabilityRequirements() {
        return List.of(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.adb, MAJOR_ADB_VERSION, MINOR_ADB_VERSION));
    }

    @Override
    public void wakeUpDevice(DeviceInfo deviceInfo, Logger logger) {
        sendKeyEvent(deviceInfo, KEYCODE_WAKEUP, logger);
    }

    @Override
    public void unlockDevice(@NotNull DeviceInfo deviceInfo, @Nullable Logger logger) {
        sendKeyEvent(deviceInfo, KEYCODE_MENU, logger);
    }

    @Override
    public void backToHome(DeviceInfo deviceInfo, Logger logger) {
        sendKeyEvent(deviceInfo, KEYCODE_HOME, logger);
    }

    @Override
    /**
     * For more details about Android Permission: https://developer.android.com/guide/topics/permissions/overview#runtime
     * From source code perspective, this method will call into:
     * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/pm/permission/PermissionManagerServiceImpl.java;drc=fddf7bbb1ec61ebb4f163733c5743c7260f1af51;l=1323
     * The Android Permissions are held and registered to frameworks/base/services/core/java/com/android/server/pm/permission/PermissionRegistry.java.
     * They are loaded by frameworks/base/core/java/com/android/server/SystemConfig.java readPermissions methods line ~727 from file etc/permissions/platform.xml under the libraryDir
     *
     * May refer to frameworks/base/core/java/android/content/pm/PermissionInfo.java to understand the protection level of a permission.
     * And refer to cts/common/device-side/bedstead/nene/common/src/main/java/com/android/bedstead/nene/permissions/CommonPermissions.java get all the common permission names.
     * Refer to https://developer.android.com/reference/android/Manifest.permission#ACCEPT_HANDOVER to see the permission level of each permission.
     */
    public void grantPermission(@NotNull DeviceInfo deviceInfo, @NotNull String packageName,
                                @NotNull String permissionName, @Nullable Logger logger) {
        try {
            adbOperateUtil.execOnDevice(deviceInfo, String.format("pm grant %s %s", packageName, permissionName),
                    new MultiLineNoCancelLoggingReceiver(logger), logger);
        } catch (Exception e) {
            Logger myLogger = classLogger;
            if (logger != null) {
                myLogger = logger;
            }
            myLogger.error(e.getMessage(), e);
        }
    }

    /**
     * Grant all the permissions needed for an APK
     *
     * @param deviceInfo
     * @param packageFile
     * @param targetPackage
     * @param allowCustomizedPermissions We currently assume if a permission name contains "android.", it's a system (app) defined Android permission.
     * @param logger                     may be null
     * @return return true if succeeded.
     */
    @Override
    public boolean grantAllPackageNeededPermissions(@NotNull DeviceInfo deviceInfo, @NotNull File packageFile,
                                                    @NotNull String targetPackage,
                                                    boolean allowCustomizedPermissions, @Nullable Logger logger) {
        Logger myLogger = classLogger;
        if (logger != null) {
            myLogger = logger;
        }
        try (ApkFile apkFile = new ApkFile(packageFile)) {
            ApkMeta meta = apkFile.getApkMeta();
            for (String usesPermission : meta.getUsesPermissions()) {
                if (StringUtils.isEmpty(usesPermission)) {
                    continue;
                }
                if (!allowCustomizedPermissions && !isAndroidCommonPermission(usesPermission)) {
                    myLogger.info("Skip permission {}", usesPermission);
                    continue;
                }
                try {
                    grantPermission(deviceInfo, targetPackage, usesPermission, logger);
                } catch (Exception e) {
                    myLogger.info(String.format("error occurred when granting permission %s to package %s",
                            usesPermission, targetPackage), e);
                }
            }
            return true;
        } catch (IOException e) {
            myLogger.warn(
                    "grantAllPackageNeededPermissions for file failed, file path: " + packageFile.getAbsolutePath(),
                    e);
        }
        return false;
    }

    /**
     * Grant all the permissions needed for a test task
     *
     * @param deviceInfo
     * @param task
     * @param logger     may be null
     * @return return true if succeeded.
     */
    @Override
    public boolean grantAllTaskNeededPermissions(@NotNull DeviceInfo deviceInfo, @NotNull TestTask task,
                                                 @Nullable Logger logger) {
        Logger myLogger = classLogger;
        if (logger != null) {
            myLogger = logger;
        }
        HashSet<String> permissionToGrant = new HashSet<>();
        boolean succeeded = true;
        if (task.getNeededPermissions() != null) {
            permissionToGrant.addAll(task.getNeededPermissions());
        }
        try (ApkFile apkFile = new ApkFile(task.appFile)) {
            ApkMeta meta = apkFile.getApkMeta();
            permissionToGrant.addAll(meta.getUsesPermissions());
        } catch (IOException e) {
            myLogger.warn("Parsing the apk file failed, file path: " + task.appFile.getAbsolutePath(), e);
            succeeded = false;
        }
        for (String usesPermission : permissionToGrant) {
            if (StringUtils.isEmpty(usesPermission)) {
                continue;
            }
            if (!task.shouldGrantCustomizedPermissions() && !isAndroidCommonPermission(usesPermission)) {
                continue;
            }
            try {
                grantPermission(deviceInfo, task.getPkgName(), usesPermission, logger);
            } catch (Exception e) {
                myLogger.info(
                        String.format("error occurred when granting permission %s to package %s", usesPermission,
                                task.getPkgName()), e);
            }
        }
        return succeeded;
    }

    @Override
    public void addToBatteryWhiteList(DeviceInfo deviceInfo, String packageName, Logger logger) {
        try {
            adbOperateUtil.execOnDevice(deviceInfo, String.format("dumpsys deviceidle whitelist +%s", packageName),
                    new MultiLineNoCancelLoggingReceiver(logger), logger);
        } catch (RuntimeException e) {
            classLogger.error(e.getMessage(), e);
        }
    }

    @Override
    /**
     * https://developer.android.com/studio/command-line/adb#pm
     * This method is leveraging the pm install command to do the operation.
     * -d: Allow version code downgrade.
     * -r: Reinstall an existing app, keeping its data.
     * -t: Allow test APKs to be installed.
     * -g: Grant all permissions listed in the app manifest.
     */
    public boolean installApp(DeviceInfo deviceInfo, String packagePath, @Nullable Logger logger) {
        File apk = new File(packagePath);
        Assert.isTrue(apk.exists(), "apk not exist!!");
        return adbOperateUtil.installApp(deviceInfo, apk.getAbsolutePath(), true, "-t -d -g", logger);
    }

    @Override
    public boolean uninstallApp(DeviceInfo deviceInfo, String packageName, Logger logger) {
        return adbOperateUtil.uninstallApp(deviceInfo, packageName, logger);
    }

    @Override
    public void resetPackage(DeviceInfo deviceInfo, String packageName, Logger logger) {
        try {
            adbOperateUtil.execOnDevice(deviceInfo, String.format("pm clear %s", packageName),
                    new MultiLineNoCancelLoggingReceiver(logger), logger);
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
        }
    }

    @Override
    public void pushFileToDevice(@NotNull DeviceInfo deviceInfo, @NotNull String pathOnAgent,
                                 @NotNull String pathOnDevice, @Nullable Logger logger)
            throws IOException, InterruptedException {
        adbOperateUtil.pushFileToDevice(deviceInfo, pathOnAgent, pathOnDevice, logger);
    }

    @Override
    public void pullFileFromDevice(@NotNull DeviceInfo deviceInfo, @NotNull String pathOnDevice,
                                   @Nullable Logger logger) throws IOException, InterruptedException {
        ITestRun testRun = TestRunThreadContext.getTestRun();
        Assert.notNull(testRun, "There is no testRun instance in ThreadContext!");
        Assert.notNull(testRun.getResultFolder(),
                "The testRun instance in ThreadContext does not have resultFolder property!");

        String pathOnAgent = testRun.getResultFolder().getAbsolutePath() + "/";
        adbOperateUtil.pullFileToDir(deviceInfo, pathOnAgent, pathOnDevice, logger);
    }

    @Override
    public ScreenRecorder getScreenRecorder(DeviceInfo deviceInfo, File folder, Logger logger) {
        return new PhoneAppScreenRecorder(this, this.adbOperateUtil, deviceInfo, folder, logger);
    }

    @Override
    public ADBLogcatCollector getLogCollector(DeviceInfo deviceInfo, String pkgName, TestRun testRun,
                                              Logger logger) {
        return new ADBLogcatCollector(this.adbOperateUtil, deviceInfo, pkgName, testRun, logger);
    }

    @Override
    public void setProperty(DeviceInfo deviceInfo, String property, String val, Logger logger) {
        try {
            adbOperateUtil.execOnDevice(deviceInfo, String.format("setprop %s %s", property, val),
                    new MultiLineNoCancelLoggingReceiver(logger), logger);
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
        }
    }

    public void changeGlobalSetting(DeviceInfo deviceInfo, String property, String val, Logger logger) {
        try {
            adbOperateUtil.execOnDevice(deviceInfo, String.format("settings put global %s %s", property, val),
                    new MultiLineNoCancelLoggingReceiver(logger), logger);
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
        }
    }

    public void changeSystemSetting(DeviceInfo deviceInfo, String property, String val, Logger logger) {
        try {
            adbOperateUtil.execOnDevice(deviceInfo, String.format("settings put system %s %s", property, val),
                    new MultiLineNoCancelLoggingReceiver(logger), logger);
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
        }
    }


    private void enableTouchPositionDisplay(DeviceInfo deviceInfo, Logger logger) {
        //changeSystemSetting(deviceInfo, "show_touches", "1", logger);
        changeSystemSetting(deviceInfo, "pointer_location", "1", logger);
    }

    private void deviceInfoUpdate(IDevice device) {
        DeviceState state = device.getState();
        DeviceInfo deviceInfo;
        synchronized (adbDeviceInfoMap) {
            deviceInfo = adbDeviceInfoMap.get(device.getSerialNumber());
        }
        if (deviceInfo == null) {
            if (state == DeviceState.ONLINE) {
                deviceInfo = getADBDeviceInfoFromDevice(device);
                synchronized (adbDeviceInfoMap) {
                    adbDeviceInfoMap.put(device.getSerialNumber(), deviceInfo);
                }

                try {
                    getScreenShot(deviceInfo, classLogger);
                } catch (Exception e) {
                    classLogger.error(e.getMessage(), e);
                }
            }
        } else {
            updateADBDeviceInfoByDevice(device, deviceInfo);
        }
    }

    @Override
    public boolean setDefaultLauncher(DeviceInfo deviceInfo, String packageName, String defaultActivity,
                                      Logger logger) {
        try {
            adbOperateUtil.execOnDevice(deviceInfo,
                    String.format("cmd package set-home-activity %s/%s", packageName, defaultActivity),
                    new MultiLineNoCancelLoggingReceiver(logger), logger);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void screenCapture(DeviceInfo deviceInfo, String outputFile, Logger logger) throws Exception {
        RawImage image = adbOperateUtil.getScreenshot(deviceInfo, logger);
        if (image == null) {
            return;
        }
        BufferedImage screenshot = toBufferedImage(image);
        ImgUtil.scale(screenshot, new File(outputFile), 0.7f);
    }

    private BufferedImage toBufferedImage(RawImage rawImage) {
        BufferedImage image = new BufferedImage(rawImage.width, rawImage.height, BufferedImage.TYPE_INT_ARGB);
        int index = 0;
        int indexInc = rawImage.bpp >> 3;
        for (int y = 0; y < rawImage.height; y++) {
            for (int x = 0; x < rawImage.width; x++) {
                int value = rawImage.getARGB(index) | 0xff000000;
                index += indexInc;
                image.setRGB(x, y, value);
            }
        }

        return image;
    }

    private boolean sendKeyEvent(DeviceInfo deviceInfo, String event, Logger logger) {

        try {
            adbOperateUtil.execOnDevice(deviceInfo, String.format("input keyevent %s", event),
                    new MultiLineNoCancelLoggingReceiver(logger), logger);
            return true;
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
            return false;
        }
    }

    private DeviceInfo getADBDeviceInfoFromDevice(IDevice device) {
        DeviceInfo adbDevice = new DeviceInfo();
        updateADBDeviceInfoByDevice(device, adbDevice);
        return adbDevice;
    }

    private void updateADBDeviceInfoByDevice(IDevice device, DeviceInfo adbDevice) {
        adbDevice.setSerialNum(device.getSerialNumber());

        DeviceState state = device.getState();
        String stateStr;
        if (state == null) {
            stateStr = DeviceInfo.OFFLINE;
        } else {
            stateStr = state.toString();
        }
        if (!(adbDevice.isTesting() && stateStr.equals(DeviceInfo.ONLINE) || adbDevice.isUnstable())) {
            adbDevice.setStatus(stateStr);
        }

        if (stateStr.equals(DeviceInfo.ONLINE)) {
            String model = device.getProperty(PROP_DEVICE_MODEL);
            if (model == null) {
                adbDevice.setModel(device.getName());
            }
            adbDevice.setModel(model);
            adbDevice.setOsSDKInt(device.getProperty(PROP_BUILD_API_LEVEL));
            adbDevice.setOsVersion(device.getProperty(PROP_BUILD_VERSION));
            adbDevice.setManufacturer(device.getProperty(PROP_DEVICE_MANUFACTURER));
            adbDevice.setBrand(device.getProperty("ro.product.brand"));
            adbDevice.setAbiList(device.getProperty(PROP_DEVICE_CPU_ABI_LIST));
            String screenSize = getScreenSize(adbDevice, null);
            String deviceId = getDeviceId(adbDevice, null);
            adbDevice.setScreenSize(screenSize);
            adbDevice.setDeviceId(deviceId);
            adbDevice.setScreenDensity(device.getDensity());
            adbDevice.setName(device.getName());
            adbDevice.setType(DeviceType.ANDROID.name());
        }
    }

    private String getScreenSize(DeviceInfo deviceInfo, Logger logger) {
        // Physical size: 1440x3120
        // Override size: 1080x2340
        final String[] realSize = {null};
        try {
            adbOperateUtil.execOnDevice(deviceInfo, "wm size", new MultiLineNoCancelReceiver() {
                @Override
                public void processNewLines(String[] lines) {
                    for (String line : lines) {
                        if (StringUtils.isBlank(line)) {
                            continue;
                        }
                        String s = "Override size:";
                        if (line.contains(s)) {
                            realSize[0] = line.substring(s.length() + 1).trim();
                            return;
                        }
                        s = "Physical size:";
                        if (line.contains(s)) {
                            realSize[0] = line.substring(s.length() + 1).trim();
                        }
                    }
                }

            }, logger);
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
        }

        return realSize[0];
    }

    private String getDeviceId(DeviceInfo deviceInfo, Logger logger) {
        try {
            final String[] idOutput = {null};
            adbOperateUtil.execOnDevice(deviceInfo, "settings get secure android_id",
                    new MultiLineNoCancelReceiver() {
                        @Override
                        public void processNewLines(String[] lines) {
                            for (String line : lines) {
                                String trim = line.trim();
                                if (StringUtils.isBlank(trim)) {
                                    continue;
                                }
                                idOutput[0] = trim;
                            }
                        }
                    }, logger);
            return idOutput[0];
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
            return null;
        }
    }


    @Override
    public boolean isAppInstalled(DeviceInfo deviceInfo, String packageName, Logger logger) {
        Assert.isTrue(deviceInfo.isAlive());
        final boolean[] locked = {false};

        adbOperateUtil.execOnDevice(deviceInfo, "pm list packages", new MultiLineNoCancelReceiver() {
            @Override
            public void processNewLines(@NotNull String[] lines) {
                for (String line : lines) {
                    if (StringUtils.isBlank(line)) {
                        continue;
                    }
                    if (line.contains(packageName)) {
                        logger.info(line);
                        locked[0] = true;
                        break;
                    }
                }
            }
        }, logger);

        logger.info("checking device lock state {}, result {}", packageName, locked[0]);
        return locked[0];
    }

    @Override
    public boolean grantProjectionAndBatteryPermission(DeviceInfo deviceInfo, String recordPackageName, Logger logger) {
        boolean isProjectionPermissionGranted = false;
        stopPackageProcess(deviceInfo, recordPackageName, logger);
        wakeUpDevice(deviceInfo, logger);
        unlockDevice(deviceInfo, logger);
        startRecordActivity(deviceInfo, logger);

        while (true) {
            ThreadUtils.safeSleep(2000);
            if (clickNodeOnDeviceWithText(deviceInfo, logger, "Allow display over other apps")) {
                sendKeyEvent(deviceInfo, KEYCODE_BACK, logger);
                sendKeyEvent(deviceInfo, KEYCODE_HOME, logger);
                stopPackageProcess(deviceInfo, recordPackageName, logger);
                startRecordActivity(deviceInfo, logger);
            } else if (clickNodeOnDeviceWithText(deviceInfo, logger, "Start now", "Allow", "允许")) {
                isProjectionPermissionGranted = true;
            } else {
                break;
            }
        }
        return isProjectionPermissionGranted;
    }

    private String dumpView(DeviceInfo deviceInfo, Logger logger) {
        StringBuilder sb = new StringBuilder();
        adbOperateUtil.execOnDevice(deviceInfo, "uiautomator dump", new MultiLineNoCancelLoggingReceiver(logger),
                logger);
        adbOperateUtil.execOnDevice(deviceInfo, "cat /sdcard/window_dump.xml", new MultiLineNoCancelReceiver() {
            @Override
            public void processNewLines(@NotNull String[] lines) {
                for (String line : lines) {
                    sb.append(line);
                }
            }
        }, logger);
        return sb.toString();
    }

    private void stopPackageProcess(DeviceInfo deviceInfo, String packageName, Logger logger) {
        try {
            adbOperateUtil.execOnDevice(Objects.requireNonNull(deviceInfo), "am force-stop " + packageName,
                    new MultiLineNoCancelLoggingReceiver(logger), logger);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void startRecordActivity(DeviceInfo deviceInfo, Logger logger) {
        try {
            adbOperateUtil.execOnDevice(Objects.requireNonNull(deviceInfo),
                    "am start -n " + recordPackageName + "/.MainActivity",
                    new MultiLineNoCancelLoggingReceiver(logger), logger);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private boolean clickNodeOnDeviceWithText(DeviceInfo deviceInfo, Logger logger,
                                              @NotNull String... possibleTexts) {
        String dump = dumpView(deviceInfo, logger);
        // classLogger.info("Dump on {}: {}", adbDeviceInfo.getSerialNum(), dump);
        if (StringUtils.isBlank(dump)) {
            logger.error("did not find element with text {} on {}", Arrays.asList(possibleTexts).toString(),
                    deviceInfo.getSerialNum());
            return false;
        }
        Document viewTree = Jsoup.parse(dump, "", Parser.xmlParser());
        for (String possibleText : possibleTexts) {
            Elements startNowNode = viewTree.select(String.format("node[text=\"%s\"]", possibleText));
            if (!startNowNode.isEmpty()) {
                Element element = startNowNode.get(0);
                String bounds = element.attr("bounds");
                String[] boundsVal = bounds.split("[\\[\\],]+");
                int xStart = Integer.parseInt(boundsVal[1]);
                int yStart = Integer.parseInt(boundsVal[2]);
                int xEnd = Integer.parseInt(boundsVal[3]);
                int yEnd = Integer.parseInt(boundsVal[4]);
                int clickX = (xStart + xEnd) / 2;
                int clickY = (yStart + yEnd) / 2;
                adbOperateUtil.clickOnDeviceAbsoluteXY(deviceInfo, clickX, clickY, logger);
                return true;
            }
        }
        return false;
    }

    @Override
    public void testDeviceSetup(@NotNull DeviceInfo deviceInfo, Logger logger) {
        changeGlobalSetting(deviceInfo, "window_animation_scale", "0", logger);
        changeGlobalSetting(deviceInfo, "transition_animation_scale", "0", logger);
        changeGlobalSetting(deviceInfo, "animator_duration_scale", "0", logger);

        changeSystemSetting(deviceInfo, "screen_off_timeout", String.valueOf(TimeUnit.MINUTES.toMillis(3)), logger);

        enableTouchPositionDisplay(deviceInfo, logger);
    }

    @Override
    public void testDeviceUnset(DeviceInfo deviceInfo, Logger logger) {
        changeGlobalSetting(deviceInfo, "window_animation_scale", "1", logger);
        changeGlobalSetting(deviceInfo, "transition_animation_scale", "1", logger);
        changeGlobalSetting(deviceInfo, "animator_duration_scale", "1", logger);

        changeSystemSetting(deviceInfo, "screen_off_timeout", String.valueOf(TimeUnit.SECONDS.toMillis(45)),
                logger);
    }

    @Override
    public WebDriver getAppiumDriver(DeviceInfo deviceInfo, Logger logger) {
        return appiumServerManager.getAndroidDriver(deviceInfo, logger);
    }

    @Override
    public void quitAppiumDriver(DeviceInfo deviceInfo, Logger logger) {
        appiumServerManager.quitAndroidDriver(deviceInfo, logger);
    }

    @Override
    public void execCommandOnDevice(DeviceInfo deviceInfo, String command, Logger logger) {
        adbOperateUtil.execOnDevice(deviceInfo, command, new MultiLineNoCancelLoggingReceiver(logger), logger);
    }

    @Override
    public void removeFileInDevice(DeviceInfo deviceInfo, String pathOnDevice, Logger logger) {
        try {
            adbOperateUtil.execOnDevice(deviceInfo, "rm " + pathOnDevice,
                    new MultiLineNoCancelLoggingReceiver(logger), logger);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void networkTestStart(DeviceInfo deviceInfo, String rule, Logger logger) {
        // launch vpn
        String command_launch = "adb shell am start";
        command_launch += " -a com.microsoft.hydralab.android.client.vpn.START";
        command_launch += " -n com.microsoft.hydralab.android.client/.MainActivity";
        ShellUtils.execLocalCommandWithResult(command_launch, logger);
        while (true) {
            ThreadUtils.safeSleep(1000);
            boolean clicked = clickNodeOnDeviceWithText(deviceInfo, logger, "START NOW", "OK", "ALLOW");
            if (!clicked) {
                break;
            }
        }

        // start vpn
        String command_start = "adb shell am start";
        command_start += " -a com.microsoft.hydralab.android.client.vpn.START";
        command_start += " -n com.microsoft.hydralab.android.client/.MainActivity";
        command_start += String.format(" --es \"apps\" \"%s\"", rule);
        command_start += " --es \"output\" \"/Documents/dump.log\"";
        ShellUtils.execLocalCommandWithResult(command_start, logger);
        while (true) {
            ThreadUtils.safeSleep(1000);
            boolean clicked = clickNodeOnDeviceWithText(deviceInfo, logger, "START NOW", "OK", "ALLOW");
            if (!clicked) {
                break;
            }
        }
    }

    @Override
    public void networkTestStop(DeviceInfo deviceInfo, @NotNull File folder, Logger logger) {
        // stop vpn
        String command_stop = "adb shell am start";
        command_stop += " -a com.microsoft.hydralab.android.client.vpn.STOP";
        command_stop += " -n com.microsoft.hydralab.android.client/.MainActivity";
        ShellUtils.execLocalCommandWithResult(command_stop, logger);
        ThreadUtils.safeSleep(2000);

        // pull result
        String local_dump_path = folder.getAbsolutePath() + "/network_dump.log";
        String command_result = "adb pull /sdcard/Documents/dump.log " + local_dump_path;
        ShellUtils.execLocalCommandWithResult(command_result, logger);
        ThreadUtils.safeSleep(2000);

        // parse
        String local_result_path = folder.getAbsolutePath() + "/network_result.log";
        String[] lines;
        try {
            int count = 0;
            lines = Files.readAllLines(Paths.get(local_dump_path)).toArray(new String[0]);
            for (String line : lines) {
                if (!line.isEmpty()) {
                    ++count;
                }
            }
            File file = new File(local_result_path);
            FileWriter writer = new FileWriter(file);
            writer.write(count > 0 ? "Fail" : "Success");
            writer.close();
        } catch (IOException e) {
            // todo
        }
    }
}