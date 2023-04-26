// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.performance;

import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PerformanceTestManagementServiceTest {
    private final PerformanceTestManagementService performanceTestManagementService = new PerformanceTestManagementService();

    @Test
    public void testInspect_ReturnNull() {
        Assertions.assertNull(performanceTestManagementService.inspect(null));
    }

    @Test
    public void testParse_ReturnNull() {
        Assertions.assertNull(performanceTestManagementService.parse(null));
    }

    @Test
    public void testInspectEvent_ReturnNull() {
        PerformanceInspection eventStartInspection = PerformanceInspection.createEventStartInspection("event start");
        PerformanceInspectionResult eventStartResult = performanceTestManagementService.inspect(eventStartInspection);
        Assertions.assertNull(eventStartResult);

        PerformanceInspection eventEndInspection = PerformanceInspection.createEventEndInspection("event end");
        PerformanceInspectionResult eventEndResult = performanceTestManagementService.inspect(eventEndInspection);
        Assertions.assertNull(eventEndResult);

    }
}
