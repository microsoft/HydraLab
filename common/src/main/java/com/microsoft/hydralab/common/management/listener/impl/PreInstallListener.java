// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.management.listener.impl;

import com.android.ddmlib.InstallException;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.management.listener.DeviceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author zhoule
 * @date 01/17/2023
 */

public class PreInstallListener implements DeviceListener {
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
            if (appFile.isFile()) {
                try {
                    deviceManager.installApp(deviceInfo, appFile.getAbsolutePath(), classLogger);
                } catch (InstallException e) {
                    classLogger.error(String.format("Pre-Install %s failed", appFile.getAbsolutePath()), e);
                }
            }
        }
    }
}
