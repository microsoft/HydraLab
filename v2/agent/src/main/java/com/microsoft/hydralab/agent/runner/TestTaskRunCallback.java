// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.common.entity.common.Task;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import org.slf4j.Logger;

public interface TestTaskRunCallback {
    void onTaskStart(Task task);

    void onTaskComplete(Task task);

    void onOneDeviceComplete(Task task, TestRunDevice testRunDevice, Logger logger, TestRun result);

    void onDeviceOffline(Task task);
}