package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.common.entity.common.TestReport;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * We use this manager as the TestRunner factory and this manages all the dependencies of the TestRunner.
 */
public class TestRunnerManager {

    private final Map<String, TestRunEngine> testRunEngineMap = new HashMap<>();

    public TestReport runTestTask(TestTask testTask, TestRunDevice testRunDevice) {
        TestRunEngine testRunEngine = testRunEngineMap.get(testTask.getRunningType());
        Assert.notNull(testRunEngine, "TestRunEngine is not found for test task: " + testTask.getRunningType());
        return testRunEngine.run(testTask, testRunDevice);
    }
}
