// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.management;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.device.TestDeviceManager;
import com.microsoft.hydralab.common.management.listener.DeviceStatusListener;
import com.microsoft.hydralab.common.management.listener.DeviceStatusListenerManager;
import com.microsoft.hydralab.common.util.blob.BlobStorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zhoule
 * @date 02/22/2023
 */

public class AgentManagementService {
    static final Logger classLogger = LoggerFactory.getLogger(AgentManagementService.class);
    private final Map<String, DeviceInfo> deviceInfoMap = new HashMap<>();
    protected File testBaseDir;
    protected String testBaseDirUrlMapping;
    protected File deviceLogBaseDir;
    protected File screenshotDir;
    protected File preAppDir;
    protected String preInstallPolicy;
    protected DeviceStatusListenerManager deviceStatusListenerManager;
    protected String deviceFolderUrlPrefix;
    protected String deviceStoragePath;
    protected BlobStorageClient blobStorageClient;
    protected ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public BlobStorageClient getBlobStorageClient() {
        return blobStorageClient;
    }

    public void setBlobStorageClient(BlobStorageClient blobStorageClient) {
        this.blobStorageClient = blobStorageClient;
    }

    public DeviceStatusListenerManager getDeviceStatusListenerManager() {
        return deviceStatusListenerManager;
    }

    public void setDeviceStatusListenerManager(DeviceStatusListenerManager deviceStatusListenerManager) {
        deviceStatusListenerManager.registerListener(new DeviceStatusListener() {

            @Override
            public void onDeviceInactive(DeviceInfo deviceInfo) {
                deviceInfoMap.remove(deviceInfo.getDeviceId());
            }

            @Override
            public void onDeviceConnected(DeviceInfo deviceInfo) {
                deviceInfoMap.put(deviceInfo.getDeviceId(), deviceInfo);
            }
        });
        this.deviceStatusListenerManager = deviceStatusListenerManager;
    }

    public String getPreInstallPolicy() {
        return preInstallPolicy;
    }

    public void setPreInstallPolicy(String preInstallPolicy) {
        this.preInstallPolicy = preInstallPolicy;
    }

    public File getPreAppDir() {
        return preAppDir;
    }

    public void setPreAppDir(File preAppDir) {
        this.preAppDir = preAppDir;
    }

    public String getDeviceFolderUrlPrefix() {
        return deviceFolderUrlPrefix;
    }

    public void setDeviceFolderUrlPrefix(String deviceFolderUrlPrefix) {
        this.deviceFolderUrlPrefix = deviceFolderUrlPrefix;
    }

    public String getDeviceStoragePath() {
        return deviceStoragePath;
    }

    public void setDeviceStoragePath(String deviceStoragePath) {
        this.deviceStoragePath = deviceStoragePath;
    }

    public File getScreenshotDir() {
        return screenshotDir;
    }

    public void setScreenshotDir(File screenshotDir) {
        this.screenshotDir = screenshotDir;
    }

    public File getTestBaseDir() {
        return testBaseDir;
    }

    public void setTestBaseDir(File testBaseDir) {
        this.testBaseDir = testBaseDir;
    }

    public void setTestBaseDirUrlMapping(String testBaseDirUrlMapping) {
        this.testBaseDirUrlMapping = testBaseDirUrlMapping;
    }


    public File getDeviceLogBaseDir() {
        return deviceLogBaseDir;
    }

    public void setDeviceLogBaseDir(File deviceLogBaseDir) {
        this.deviceLogBaseDir = deviceLogBaseDir;
    }

    public String getTestBaseRelPathInUrl(File report) {
        return report.getAbsolutePath().replace(testBaseDir.getAbsolutePath(), testBaseDirUrlMapping).replace(File.separator, "/");
    }

    public Set<DeviceInfo> getDeviceList(Logger log) {
        Set<DeviceInfo> set = new HashSet<>();
        Set<Map.Entry<String, DeviceInfo>> entries = null;
        synchronized (this) {
            entries = new HashSet<>(deviceInfoMap.entrySet());
        }
        for (Map.Entry<String, DeviceInfo> entry : entries) {
            DeviceInfo value = entry.getValue();
            if (value != null) {
                set.add(value);
            }
        }
        return set;
    }

    public Set<DeviceInfo> getActiveDeviceList(Logger log) {
        Set<DeviceInfo> set = new HashSet<>();
        Set<Map.Entry<String, DeviceInfo>> entries = null;
        synchronized (this) {
            entries = new HashSet<>(deviceInfoMap.entrySet());
        }
        for (Map.Entry<String, DeviceInfo> entry : entries) {
            DeviceInfo value = entry.getValue();
            if (value == null || !value.isAlive()) {
                classLogger.debug("Invalid device: {}", value);
                continue;
            }
            set.add(value);
        }
        return set;
    }

    public void resetDeviceByTestId(String testId, Logger logger) {
        Set<DeviceInfo> devices = getActiveDeviceList(logger).stream()
                .filter(adbDeviceInfo -> testId.equals(adbDeviceInfo.getRunningTaskId()))
                .collect(Collectors.toSet());
        for (DeviceInfo device : devices) {
            resetDevice(device.getSerialNum());
        }
    }

    private void resetDevice(String serialNum) {
        DeviceInfo deviceInfo = deviceInfoMap.get(serialNum);
        if (deviceInfo == null) {
            return;
        }
        synchronized (deviceInfo) {
            deviceInfo.reset();
        }
    }

    public void initDeviceManager() {
        applicationContext.getBeansOfType(TestDeviceManager.class).values().forEach(TestDeviceManager::init);
    }

    public TestDeviceManager getDeviceManager(DeviceInfo deviceInfo) {
        return applicationContext.getBean(DeviceInfo.DeviceType.valueOf(deviceInfo.getType()).getBeanName(), TestDeviceManager.class);
    }

    public void updateIsPrivateByDeviceSerial(String deviceSerial, Boolean isPrivate) {
        DeviceInfo deviceInfo;
        synchronized (deviceInfoMap) {
            deviceInfo = deviceInfoMap.get(deviceSerial);
        }
        if (deviceInfo != null) {
            deviceInfo.setIsPrivate(isPrivate);
        }
    }
}
