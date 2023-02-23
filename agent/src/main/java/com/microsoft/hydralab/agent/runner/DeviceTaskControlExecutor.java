// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.common.entity.agent.DeviceTaskControl;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.util.ThreadPoolUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

@Service
public class DeviceTaskControlExecutor {
    static final Logger log = LoggerFactory.getLogger(DeviceTaskControlExecutor.class);
    @Resource
    AgentManagementService agentManagementService;

    @Nullable
    public DeviceTaskControl runForAllDeviceAsync(Collection<DeviceInfo> allDevices, DeviceTask task, TaskCompletion taskCompletion) {
        //the status of device will be controlled by master, so the task will run no matter what the status saved in agent is
        return runForAllDeviceAsync(allDevices, task, taskCompletion, true, true);
    }

    public DeviceTaskControl runForAllDeviceAsync(Collection<DeviceInfo> allDevices, DeviceTask task, TaskCompletion taskCompletion, boolean logging, boolean forceForTesting) {
        int activeDevice = 0;
        log.warn("All device count {}", allDevices.size());
        for (DeviceInfo device : allDevices) {
            if (!device.isAlive()) {
                log.warn("Device {} not alive", device.getSerialNum());
                continue;
            }
            if (device.isTesting() && !forceForTesting) {
                log.warn("Device {} is under testing", device.getSerialNum());
                continue;
            }
            activeDevice++;
        }

        if (activeDevice <= 0) {
            log.warn("No device available for this task, forceForTesting: {}", forceForTesting);
            return null;
        }

        CountDownLatch count = new CountDownLatch(activeDevice);
        final Set<DeviceInfo> devices = new HashSet<>();

        if (logging) {
            log.info("RunForAllDeviceAsync: on {} devices", allDevices.size());
        }

        for (DeviceInfo device : allDevices) {
            if (!device.isAlive()) {
                log.info("RunForAllDeviceAsync: device not alive: {}", device.getName());
                continue;
            }
            if (device.isTesting() && !forceForTesting) {
                log.info("RunForAllDeviceAsync: [BUSY] device is testing: {}", device.getName());
                continue;
            }
            Logger logger = null;
            if (logging) {
                logger = agentManagementService.getDeviceManager(device).getDeviceLogger(device);
            }
            final Logger fLogger = logger;
            devices.add(device);
            Runnable run = () -> {
                try {
                    if (logging) {
                        log.info("start do task: {}", device.getName());
                    }
                    task.doTask(device, fLogger);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    count.countDown();
                    if (count.getCount() <= 0 && taskCompletion != null) {
                        taskCompletion.onComplete();
                    }
                }
            };
            ThreadPoolUtil.TEST_EXECUTOR.execute(run);
        }
        return new DeviceTaskControl(count, devices);
    }

    public interface DeviceTask {
        boolean doTask(DeviceInfo deviceInfo, Logger logger) throws Exception;
    }

    public interface TaskCompletion {
        void onComplete();
    }
}
