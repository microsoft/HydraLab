package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestTask;
/**
 * We use this interface to define the test runner exposed behavior and I/O.
 */
public interface TestEngine {
    /**
     * Start the test run by calling the lifecycle {@link TestLifecycle}.
     * @param testTask the test task to run
     * @param deviceInfo the device to run the test task
     */
    void start(TestTask testTask, DeviceInfo deviceInfo);
}
