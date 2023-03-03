// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.screen;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.DeviceManager;
import io.appium.java_client.ios.IOSDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class IOSAppiumScreenRecorder implements ScreenRecorder {
    static final Logger CLASS_LOGGER = LoggerFactory.getLogger(IOSAppiumScreenRecorder.class);

    protected static final int DEFAULT_TIMEOUT_IN_SECOND = 600;
    static final String INTERRUPT_SCRIPT_PATH = "InterruptProcess.ps1";
    protected DeviceManager deviceManager;
    protected IOSDriver iosDriver;
    protected DeviceInfo deviceInfo;
    protected String recordDir;

    protected boolean isStarted = false;

    public IOSAppiumScreenRecorder(DeviceManager deviceManager, DeviceInfo info, String recordDir) {
        this.deviceManager = deviceManager;
        this.deviceInfo = info;
        this.recordDir = recordDir;

        this.iosDriver = deviceManager.getAppiumServerManager().getIOSDriver(deviceInfo, CLASS_LOGGER);
    }

    @Override
    public void setupDevice() {

    }

    @Override
    public int getPreSleepSeconds() {
        return 0;
    }

    @Override
    public abstract void startRecord(int maxTimeInSecond);

    @Override
    public abstract boolean finishRecording();
}
