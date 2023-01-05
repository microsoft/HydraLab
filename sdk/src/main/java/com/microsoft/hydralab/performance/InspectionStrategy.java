package com.microsoft.hydralab.performance;

import java.util.concurrent.TimeUnit;

/**
 * Regarding the design of `when`
 *
 * For example:
 * ```
 * strategy.when='testStart'
 * strategy.whenType=WhenType.TestLifecycle
 * ```
 * This means that this inspection should happen whenever the `testStart` lifecycle is called.
 *
 * ```
 * strategy.when='testCaseStart'
 * strategy.whenType=WhenType.TestLifecycle
 * ```
 * This means that this inspection should happen whenever the `testCaseStart` lifecycle is called.
 *
 * Regarding how we should define the test run lifecycle, please refer to `com.android.ddmlib.testrunner.ITestRunListener`.
 */
public class InspectionStrategy {
    public int interval;
    public TimeUnit intervalUnit;
    public String when;
    public int whenType;
}
