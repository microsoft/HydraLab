package com.microsoft.hydralab.common.entity.common;

public class TestResult {
    TestStatus status;
    public enum TestStatus {
        // The test passed
        PASS,
        // The test failed
        FAIL,
        /**
         * The test was skipped.
         * Skipped tests typically occur when tests are available for optional functionality. For example, a
         * test suite for video cards might include tests that run only if certain features are enabled in
         * hardware.
         */
        SKIP,
        /**
         * The test was aborted.
         * Aborted tests typically occur when a test suite is interrupted by a user or a system.
         * This is also known as a "ABORT".
         *
         * Another most common example of an aborted result occurs when expected support files are not
         * available. For example, if test collateral (additional files needed to run the test) are located on
         * a network share and that share is unavailable, the test is aborted.
         */
        CANCEL,
        /**
         * The test was blocked from running by a known application or system issue. Marking tests as
         * blocked (rather than failed) when they cannot run as a result of a known bug keeps failure
         * rates from being artificially high, but high numbers of blocked test cases indicate areas where
         * quality and functionality are untested and unknown.
         */
        BLOCK,
        /**
         * The test passed but indicated warnings that might need to be examined in greater detail.
         */
        WARNING
    }
}
