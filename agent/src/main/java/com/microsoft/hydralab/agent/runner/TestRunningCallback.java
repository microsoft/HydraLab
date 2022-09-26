// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.TestTask;
import org.slf4j.Logger;

public interface TestRunningCallback {
    void onAllComplete(TestTask testTask);

    void onOneDeviceComplete(TestTask testTask, DeviceInfo deviceControl, Logger logger, DeviceTestTask result);

    void onDeviceOffline(TestTask testTask);
}