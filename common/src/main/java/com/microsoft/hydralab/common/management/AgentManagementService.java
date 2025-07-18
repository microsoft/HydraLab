// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.management;

import com.microsoft.hydralab.common.entity.agent.AgentFunctionAvailability;
import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.agent.EnvInfo;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.management.listener.DeviceStatusListener;
import com.microsoft.hydralab.common.management.listener.DeviceStatusListenerManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    protected String registryServer;
    protected File deviceLogBaseDir;
    protected File screenshotDir;
    protected File preAppDir;
    protected DeviceStatusListenerManager deviceStatusListenerManager;
    protected String deviceFolderUrlPrefix;
    protected String deviceStoragePath;
    protected StorageServiceClientProxy storageServiceClientProxy;
    protected String preInstallFailurePolicy;
    protected EnvInfo envInfo;
    private List<AgentFunctionAvailability> functionAvailabilities = new ArrayList<>();
    private String testTempFilePath;

    public List<AgentFunctionAvailability> getFunctionAvailabilities() {
        return functionAvailabilities;
    }

    public EnvInfo getEnvInfo() {
        return envInfo;
    }

    public void setEnvInfo(EnvInfo envInfo) {
        this.envInfo = envInfo;
    }

    public StorageServiceClientProxy getStorageServiceClientProxy() {
        return storageServiceClientProxy;
    }

    public void setStorageServiceClientProxy(
            StorageServiceClientProxy storageServiceClientProxy) {
        this.storageServiceClientProxy = storageServiceClientProxy;
    }

    public String getPreInstallFailurePolicy() {
        return preInstallFailurePolicy;
    }

    public void setPreInstallFailurePolicy(String preInstallFailurePolicy) {
        this.preInstallFailurePolicy = preInstallFailurePolicy;
    }

    public DeviceStatusListenerManager getDeviceStatusListenerManager() {
        return deviceStatusListenerManager;
    }

    public void setDeviceStatusListenerManager(DeviceStatusListenerManager deviceStatusListenerManager) {
        deviceStatusListenerManager.registerListener(new DeviceStatusListener() {

            @Override
            public void onDeviceInactive(DeviceInfo deviceInfo) {
                deviceInfoMap.remove(deviceInfo.getSerialNum());
            }

            @Override
            public void onDeviceConnected(DeviceInfo deviceInfo) {
                deviceInfoMap.put(deviceInfo.getSerialNum(), deviceInfo);
            }
        });
        this.deviceStatusListenerManager = deviceStatusListenerManager;
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


    public String getRegistryServer() {
        return registryServer;
    }

    public void setRegistryServer(String registryServer) {
        this.registryServer = registryServer;
    }

    public File getDeviceLogBaseDir() {
        return deviceLogBaseDir;
    }

    public void setDeviceLogBaseDir(File deviceLogBaseDir) {
        this.deviceLogBaseDir = deviceLogBaseDir;
    }

    public String getTestBaseRelPathInUrl(File report) {
        return getTestBaseRelPathInUrl(report.getAbsolutePath());
    }

    public String getTestBaseRelPathInUrl(String path) {
        // check the path is real path
        if (path == null || !path.startsWith(testBaseDir.getAbsolutePath())) {
            classLogger.warn("Path {} is not under test base dir {}", path, testBaseDir.getAbsolutePath());
            return path.replace(File.separator, "/");
        }
        return path.replace(testBaseDir.getAbsolutePath(), testBaseDirUrlMapping)
                .replace(File.separator, "/");
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

    public void updateIsPrivateByDeviceSerial(String deviceSerial, Boolean isPrivate) {
        DeviceInfo deviceInfo;
        synchronized (deviceInfoMap) {
            deviceInfo = deviceInfoMap.get(deviceSerial);
        }
        if (deviceInfo != null) {
            deviceInfo.setIsPrivate(isPrivate);
        }
    }

    public void registerFunctionAvailability(String functionName, AgentFunctionAvailability.AgentFunctionType functionType, boolean enabled,
                                             List<EnvCapabilityRequirement> requirements) {
        boolean available = true;
        for (EnvCapabilityRequirement requirement : requirements) {
            Optional<EnvCapability> envCapability =
                    envInfo.getCapabilities().stream().filter(capability -> capability.getKeyword().equals(requirement.getEnvCapability().getKeyword())).findFirst();
            if (!envCapability.isPresent()) {
                available = false;
                continue;
            }
            boolean isReady = envCapability.get().meet(requirement.getEnvCapability());
            requirement.setReady(isReady);
            available = available && isReady;
        }
        functionAvailabilities.add(new AgentFunctionAvailability(functionName, functionType, enabled, available, requirements));
    }

    public String getEnvFilePath(EnvCapability.CapabilityKeyword keyword) {
        Optional<EnvCapability> envCapability =
                envInfo.getCapabilities().stream().filter(capability -> capability.getKeyword().equals(keyword)).findFirst();
        if (!envCapability.isPresent()) {
            return null;
        }
        return envCapability.get().getFile().getAbsolutePath();
    }

    public void setTestTempFilePath(String tempFilePath) {
        File dir = new File(tempFilePath);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("create dir fail: " + dir.getAbsolutePath());
            }
        }
        this.testTempFilePath = tempFilePath;
    }

    public String copyPreinstallAPK(String fileName) {
        File preinstallApk = new File(this.testTempFilePath, fileName);
        if (preinstallApk.exists()) {
            preinstallApk.delete();
        }
        try (InputStream resourceAsStream = FileUtils.class.getClassLoader().getResourceAsStream(fileName); OutputStream out = new FileOutputStream(preinstallApk)) {
            IOUtils.copy(Objects.requireNonNull(resourceAsStream), out);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return preinstallApk.getAbsolutePath();
    }
}