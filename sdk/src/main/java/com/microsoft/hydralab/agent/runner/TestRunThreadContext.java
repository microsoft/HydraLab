package com.microsoft.hydralab.agent.runner;

/**
 * We will gradually deprecate ThreadParam and AppiumParam, and migrate to this.
 *
 */
public class TestRunThreadContext {
    private static final InheritableThreadLocal<ITestRun> testRunThreadLocal = new InheritableThreadLocal<>();

    /**
     * Should be called in the TestRunner setup lifecycle
     * @param testRun
     */
    static void init(ITestRun testRun) {
        clean();
        testRunThreadLocal.set(testRun);
    }

    public static void clean() {
        testRunThreadLocal.remove();
    }

    public static ITestRun getTestRun() {
        return testRunThreadLocal.get();
    }
}
