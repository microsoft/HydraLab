// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.management.listener.impl;

import com.android.ddmlib.InstallException;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.management.listener.DeviceStatusListener;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.File;

/**
 * @author zhoule
 * @date 01/17/2023
 */

public class PreInstallListener implements DeviceStatusListener {
    DeviceManager deviceManager;
    private Logger classLogger = LoggerFactory.getLogger(PreInstallListener.class);

    public PreInstallListener(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    @Override
    public void onDeviceInactive(DeviceInfo deviceInfo) {

    }

    @Override
    public void onDeviceConnected(DeviceInfo deviceInfo) {
        File appDir = deviceManager.getPreAppDir();
        File[] appFiles = appDir.listFiles();
        for (File appFile : appFiles) {
            if (!appFile.isFile()) {
                continue;
            }
            try {
                deviceManager.installApp(deviceInfo, appFile.getAbsolutePath(), classLogger);
            } catch (InstallException e) {
                String errorMessage = String.format("Pre-Install %s failed", appFile.getAbsolutePath());
                classLogger.error(errorMessage, e);
                throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorMessage, e);
            }
        }
    }
}
