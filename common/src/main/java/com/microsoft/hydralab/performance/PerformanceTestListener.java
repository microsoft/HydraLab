package com.microsoft.hydralab.performance;

/**
 * The listener that is called during a performance test run
 */
public interface PerformanceTestListener {
    /**
     * Called before any tests have been run.
     *
     * @param description the description of the test that is about to be run
     */
    void testStarted(String description);

    /**
     * Called when an atomic test succeeds.
     *
     * @param description the description of the test that just ran
     */
    void testSuccess(String description);

    /**
     * Called when an atomic test fails.
     *
     * @param description describes the test that failed
     */
    void testFailure(String description);

    /**
     * Called before any tests have been run.
     */
    void testRunStarted();

    /**
     * Called when all tests have finished.
     */
    void testRunFinished();
}
