package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.common.entity.common.Task;
import com.microsoft.hydralab.common.entity.common.TestReport;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import org.springframework.util.Assert;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * We use this manager as the TestRunner factory and this manages all the dependencies of the TestRunner.
 */
public class TestRunnerManager {

    private final ConcurrentMap<String, TestRunEngine> testRunEngineMap = new ConcurrentHashMap<>();

    public void addRunEngine(Task.RunnerType runnerType, TestRunEngine testRunEngine) {
        testRunEngineMap.put(runnerType.name(), testRunEngine);
    }

    public TestReport runTestTask(Task task, TestRunDevice testRunDevice) {
        TestRunEngine testRunEngine = testRunEngineMap.get(task.getRunnerType());
        Assert.notNull(testRunEngine, "TestRunEngine is not found for test task: " + task.getRunnerType());

        TestReport testReport = testRunEngine.run(task, testRunDevice);
        return testReport;
    }
}
