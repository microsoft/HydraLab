// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.common.entity.agent.DeviceTaskControl;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.util.ThreadPoolUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

@Service
public class DeviceTaskControlExecutor {
    @SuppressWarnings("constantname")
    static final Logger log = LoggerFactory.getLogger(DeviceTaskControlExecutor.class);
    @Resource
    TestRunDeviceOrchestrator testRunDeviceOrchestrator;

    @Nullable
    public DeviceTaskControl runForAllDeviceAsync(Collection<TestRunDevice> allDevices, DeviceTask task,
                                                  TaskCompletion taskCompletion, boolean isAgentTask) {
        //the status of device will be controlled by master, so the task will run no matter what the status saved in agent is
        return runForAllDeviceAsync(allDevices, task, taskCompletion, true, true, isAgentTask);
    }

    public DeviceTaskControl runForAllDeviceAsync(Collection<TestRunDevice> allDevices, DeviceTask task,
                                                  TaskCompletion taskCompletion, boolean logging,
                                                  boolean forceForTesting, boolean isAgentTask) {
        if (isAgentTask) {
            CountDownLatch count = new CountDownLatch(1);
            TestRunDevice fakeDevice = allDevices.iterator().next();
            Runnable run = () -> {
                try {
                    if (logging) {
                        DeviceTaskControlExecutor.log.info("start do task on a fake device");
                    }
                    task.doTask(fakeDevice);
                } catch (Exception e) {
                    DeviceTaskControlExecutor.log.error(e.getMessage(), e);
                } finally {
                    count.countDown();
                    if (count.getCount() <= 0 && taskCompletion != null) {
                        taskCompletion.onComplete();
                    }
                }
            };
            ThreadPoolUtil.TEST_EXECUTOR.execute(run);

            return new DeviceTaskControl(count, Set.of(fakeDevice));
        }
        int activeDevice = 0;
        log.warn("All device count {}", allDevices.size());
        for (TestRunDevice device : allDevices) {
            if (!testRunDeviceOrchestrator.isAlive(device)) {
                log.warn("Device {} not alive", testRunDeviceOrchestrator.getSerialNum(device));
                continue;
            }
            if (testRunDeviceOrchestrator.isTesting(device) && !forceForTesting) {
                log.warn("Device {} is under testing", testRunDeviceOrchestrator.getSerialNum(device));
                continue;
            }
            activeDevice++;
        }

        if (activeDevice <= 0) {
            log.warn("No device available for this task, forceForTesting: {}", forceForTesting);
            return null;
        }

        CountDownLatch count = new CountDownLatch(activeDevice);
        final Set<TestRunDevice> devices = new HashSet<>();

        if (logging) {
            log.info("RunForAllDeviceAsync: on {} devices", allDevices.size());
        }

        for (TestRunDevice device : allDevices) {
            if (!testRunDeviceOrchestrator.isAlive(device)) {
                log.info("RunForAllDeviceAsync: device not alive: {}", testRunDeviceOrchestrator.getName(device));
                continue;
            }
            if (testRunDeviceOrchestrator.isTesting(device) && !forceForTesting) {
                log.info("RunForAllDeviceAsync: [BUSY] device is testing: {}", testRunDeviceOrchestrator.getName(device));
                continue;
            }
            if (logging) {
                device.setLogger(testRunDeviceOrchestrator.getDeviceLogger(device));
            }
            devices.add(device);
            Runnable run = () -> {
                try {
                    if (logging) {
                        DeviceTaskControlExecutor.log.info("start do task: {}", testRunDeviceOrchestrator.getName(device));
                    }
                    task.doTask(device);
                } catch (Exception e) {
                    DeviceTaskControlExecutor.log.error(e.getMessage(), e);
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
        boolean doTask(TestRunDevice testRunDevice) throws Exception;
    }

    public interface TaskCompletion {
        void onComplete();
    }
}
