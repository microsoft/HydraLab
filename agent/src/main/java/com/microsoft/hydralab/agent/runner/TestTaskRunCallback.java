// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import org.slf4j.Logger;

public interface TestTaskRunCallback {
    void onTaskStart(TestTask testTask);

    void onTaskComplete(TestTask testTask);

    void onOneDeviceComplete(TestTask testTask, DeviceInfo deviceControl, Logger logger, TestRun result);

    void onDeviceOffline(TestTask testTask);
}