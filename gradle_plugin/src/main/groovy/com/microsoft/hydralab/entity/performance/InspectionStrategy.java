// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.entity.performance;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InspectionStrategy implements Serializable {
    @SuppressWarnings("visibilitymodifier")
    public StrategyType strategyType;
    @SuppressWarnings("visibilitymodifier")
    public List<WhenType> when;
    @SuppressWarnings("visibilitymodifier")
    public long interval;
    @SuppressWarnings("visibilitymodifier")
    public TimeUnit intervalUnit;
    @SuppressWarnings("visibilitymodifier")
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
