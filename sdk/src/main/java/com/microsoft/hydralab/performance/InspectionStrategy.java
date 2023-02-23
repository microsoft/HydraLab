// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InspectionStrategy implements Serializable {
    public StrategyType strategyType;
    public List<WhenType> when;
    public long interval;
    public TimeUnit intervalUnit;
    public PerformanceInspection inspection;

    enum StrategyType {
        TEST_LIFECYCLE,
        TEST_SCHEDULE
    }

    enum WhenType {
        TEST_STARTED,
        TEST_SUCCESS,
        TEST_RUN_STARTED,
        TEST_RUN_FINISHED,
        TEST_FAILURE
    }
}
