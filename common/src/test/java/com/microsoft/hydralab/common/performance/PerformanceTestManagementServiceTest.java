// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.performance;

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
}
