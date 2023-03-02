// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner;

/**
 * We will gradually deprecate ThreadParam and AppiumParam, and migrate to this.
 */
public final class TestRunThreadContext {
    private static final InheritableThreadLocal<ITestRun> TEST_RUN_INHERITABLE_THREAD_LOCAL = new InheritableThreadLocal<>();

    private TestRunThreadContext() {

    }

    /**
     * Should be called in the TestRunner setup lifecycle
     *
     * @param testRun
     */
    static void init(ITestRun testRun) {
        clean();
        TEST_RUN_INHERITABLE_THREAD_LOCAL.set(testRun);
    }

    public static void clean() {
        TEST_RUN_INHERITABLE_THREAD_LOCAL.remove();
    }

    public static ITestRun getTestRun() {
        return TEST_RUN_INHERITABLE_THREAD_LOCAL.get();
    }
}
