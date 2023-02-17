package com.microsoft.hydralab.performance;

public interface IPerformanceListener {
    void testStarted(String testName);

    void testSuccess(String testName);

    void testFailure(String testName);

    void testRunStarted();

    void testRunFinished();
}
