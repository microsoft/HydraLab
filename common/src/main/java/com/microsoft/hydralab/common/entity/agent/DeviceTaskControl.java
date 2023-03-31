// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.agent;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class DeviceTaskControl {
    private final CountDownLatch countDownLatch;
    private final Set<DeviceInfo> devices;

    public DeviceTaskControl(CountDownLatch countDownLatch, Set<DeviceInfo> devices) {
        this.countDownLatch = countDownLatch;
        this.devices = devices;
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public Set<DeviceInfo> getDevices() {
        return devices;
    }
}
