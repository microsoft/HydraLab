package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestReport;
import com.microsoft.hydralab.common.entity.common.TestResult;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;

/**
 * We use this interface to define the test runner behaviors and test life cycle.
 * And in the book of "How we test software at Microsoft", it proposed an approach of "SEARCH"
 * to define each stage of testing. We take this approach as the basis of our test runner.
 * Yet we have our own implementation and name for each stage.
 */
public interface TestLifecycle {
    /**
     * The scope of preparing a system for test execution, including operating system installation (if
     * necessary) and configuring the application under test, can grow quickly when you consider operating
     * system version and application version constraints.
     *
     * @param testTask
     * @return the test run, which contains the test task and the device info
     */
    TestRun setup(TestTask testTask, DeviceInfo deviceInfo);

    /**
     * Running the steps of the test case is the heart of automated testing, and a variety of execution methods
     * is possible.
     * <p>
     * The scope of executing a test is the application under test and the test code. The test code
     * can be written in any language, but it must be able to communicate with the application under test.
     *
     * @param testRun contains the test task and the device info
     * @throws Exception
     */
    void execute(TestRun testRun) throws Exception;

    /**
     * The scope of teardown is similar to the scope of setup. This can also be referred to as "cleanup".
     * We may need to clean up the system after the test execution and bring the system back to its original state
     * to allow for a smooth next time execution.
     *
     * @param testRun
     */
    void teardown(TestRun testRun);

    /**
     * The scope of analyzing a test result is the test result itself. The test result can be written
     * in any format, but it must be able to communicate with the test runner.
     * After execution, some level of investigation must occur to determine the result of the test. Occasionally, analysis is simple, but
     * the criteria for determining whether a test has passed or not can be complex.
     *
     * @param testRun
     * @return the test result, there are serveral types of test result, such as:
     * pass,
     * fail,
     * block,
     * skip,
     * abort,
     * warn.
     * {@link TestResult.TestState}
     */
    TestResult analyze(TestRun testRun);

    /**
     * The scope of reporting a test result is the test result itself. The test result can be written
     * in any format, but it must be able to communicate with the test runner.
     * <p>
     * A common and effective solution is to automate the parsing of log files.
     *
     * @param testRun
     * @param testResult
     * @return the test report, which contains the processed test result and the test run in a more readable format.
     */
    TestReport report(TestRun testRun, TestResult testResult);

    /**
     * The scope of HELP may not be relevant to the test runner, we can leave it empty for now.
     * In the future, we may need to add some logic here to help the test infrastructure
     * to alert on an anomaly and automatically recover from an exceptional state.
     *
     * @param testRun
     * @param testResult
     */
    void help(TestRun testRun, TestResult testResult);
}
