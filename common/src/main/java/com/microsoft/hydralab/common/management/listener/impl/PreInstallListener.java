// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.management.listener.impl;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.device.impl.DeviceDriverManager;
import com.microsoft.hydralab.common.management.listener.DeviceStatusListener;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.FlowUtil;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.PkgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.io.File;

/**
 * @author zhoule
 * @date 01/17/2023
 */

public class PreInstallListener implements DeviceStatusListener {
    AgentManagementService agentManagementService;
    DeviceDriverManager deviceDriverManager;
    private Logger classLogger = LoggerFactory.getLogger(PreInstallListener.class);

    public PreInstallListener(AgentManagementService agentManagementService, DeviceDriverManager deviceDriverManager) {
        this.agentManagementService = agentManagementService;
        this.deviceDriverManager = deviceDriverManager;
    }

    @Override
    public void onDeviceInactive(DeviceInfo deviceInfo) {

    }

    @Override
    public void onDeviceConnected(DeviceInfo deviceInfo) {
        File appDir = agentManagementService.getPreAppDir();
        File[] appFiles = appDir.listFiles();
        for (File appFile : appFiles) {
            if (!appFile.isFile()) {
                continue;
            }
            try {
                // install app
                deviceDriverManager.installApp(deviceInfo, appFile.getAbsolutePath(), classLogger);
                classLogger.info("Pre-Install {} successfully", appFile.getAbsolutePath());
            } catch (Exception e) {
                String errorMessage = String.format("Pre-Install %s failed", appFile.getAbsolutePath());
                classLogger.error(errorMessage, e);
                try {
                    FlowUtil.retryAndSleepWhenFalse(3, 10, () -> {
                        // try to uninstall app first
                        try {
                            JSONObject res = PkgUtil.analysisFile(appFile, EntityType.APP_FILE_SET);
                            if (!StringUtils.isEmpty(res.getString(StorageFileInfo.ParserKey.PKG_NAME))) {
                                deviceDriverManager.uninstallApp(deviceInfo, res.getString(StorageFileInfo.ParserKey.PKG_NAME), classLogger);
                            }
                        } catch (Exception e1) {
                            classLogger.warn("Uninstall origin app of {} failed", appFile.getName(), e);
                        }
                        // install app
                        return deviceDriverManager.installApp(deviceInfo, appFile.getAbsolutePath(), classLogger);
                    });
                } catch (Exception e2) {
                    classLogger.warn("Uninstall origin app of {} failed", appFile.getName(), e2);
                    if (Const.PreInstallFailurePolicy.SHUTDOWN.equals(
                            agentManagementService.getPreInstallFailurePolicy())) {
                        throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorMessage, e2);
                    }
                }
            }
        }
    }
}