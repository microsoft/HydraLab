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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
                deviceInfoMap.remove(deviceInfo.getDeviceId());
            }

            @Override
            public void onDeviceConnected(DeviceInfo deviceInfo) {
                deviceInfoMap.put(deviceInfo.getDeviceId(), deviceInfo);
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


    public File getDeviceLogBaseDir() {
        return deviceLogBaseDir;
    }

    public void setDeviceLogBaseDir(File deviceLogBaseDir) {
        this.deviceLogBaseDir = deviceLogBaseDir;
    }

    public String getTestBaseRelPathInUrl(File report) {
        return report.getAbsolutePath().replace(testBaseDir.getAbsolutePath(), testBaseDirUrlMapping)
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
}