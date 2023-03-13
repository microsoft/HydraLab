// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.device.TestDevice;
import org.slf4j.Logger;

public interface TestTaskRunCallback {
    void onTaskStart(TestTask testTask);

    void onTaskComplete(TestTask testTask);

    void onOneDeviceComplete(TestTask testTask, TestDevice testDevice, Logger logger, TestRun result);

    void onDeviceOffline(TestTask testTask);
}