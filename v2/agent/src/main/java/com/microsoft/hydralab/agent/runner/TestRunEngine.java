package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.common.entity.common.Task;
import com.microsoft.hydralab.common.entity.common.TestReport;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;

/**
 * We use this interface to define the test runner exposed behavior and I/O.
 */
public interface TestRunEngine {
    /**
     * Start the test run by calling the lifecycle {@link TaskRunLifecycle}.
     *
     * @param task   the test task to run
     * @param testRunDevice the device to run the test task
     */
    TestReport run(Task task, TestRunDevice testRunDevice);
}
