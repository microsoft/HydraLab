// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner.appium;

import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.DeviceManager;
import org.slf4j.Logger;

public class AppiumCrossRunner extends AppiumRunner {
    String agentName;

    public AppiumCrossRunner(DeviceManager deviceManager, TestTaskRunCallback testTaskRunCallback, String agentName) {
        super(deviceManager, testTaskRunCallback);
        this.agentName = agentName;
    }

    @Override
    protected DeviceTestTask buildDeviceTestTask(DeviceInfo deviceInfo, TestTask testTask, Logger parentLogger) {
        DeviceTestTask deviceTestTask = super.buildDeviceTestTask(deviceInfo, testTask, parentLogger);
        String deviceName = System.getProperties().getProperty("os.name") + "-" + agentName + "-" + deviceInfo.getName();
        deviceTestTask.setDeviceName(deviceName);
        return deviceTestTask;
    }
}
