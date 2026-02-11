// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.screen;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.device.DeviceDriver;
import io.appium.java_client.ios.IOSDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class IOSAppiumScreenRecorder implements ScreenRecorder {
    static final Logger CLASS_LOGGER = LoggerFactory.getLogger(IOSAppiumScreenRecorder.class);

    protected static final int DEFAULT_TIMEOUT_IN_SECOND = 600;
    static final String INTERRUPT_SCRIPT_PATH = "InterruptProcess.ps1";
    protected DeviceDriver deviceDriver;
    protected IOSDriver iosDriver;
    protected DeviceInfo deviceInfo;
    protected String recordDir;

    protected boolean isStarted = false;
    protected boolean isDriverInitialized = false;


    public IOSAppiumScreenRecorder(DeviceDriver deviceDriver, DeviceInfo info, String recordDir) {
        this.deviceDriver = deviceDriver;
        this.deviceInfo = info;
        this.recordDir = recordDir;

        CLASS_LOGGER.info("🎬 Initializing iOS screen recorder for device: {} ({})", info.getName(), info.getSerialNum());
        try {
            this.iosDriver = deviceDriver.getAppiumServerManager().getIOSDriver(deviceInfo, CLASS_LOGGER);
            if (this.iosDriver != null) {
                CLASS_LOGGER.info("✅ IOSDriver initialized successfully for device: {}", info.getSerialNum());
                isDriverInitialized = true;
            } else {
                CLASS_LOGGER.error("❌ Failed to initialize IOSDriver - driver is null. Ensure WDA is installed on device.");
            }
        } catch (Exception e) {
            CLASS_LOGGER.error("❌ Failed to initialize IOSDriver: {}. Video recording will be disabled.", e.getMessage());
            CLASS_LOGGER.error("💡 To fix: Ensure WDA (WebDriverAgent) is installed on the device and Appium server is running.");
        }
    }

    @Override
    public void setupDevice() {

    }

    @Override
    public int getPreSleepSeconds() {
        return 0;
    }

    @Override
    abstract public void startRecord(int maxTimeInSecond);

    @Override
    abstract public String finishRecording();
}
