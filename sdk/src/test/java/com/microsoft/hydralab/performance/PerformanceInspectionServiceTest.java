package com.microsoft.hydralab.performance;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PerformanceInspectionServiceTest {
    @Test
    public void serviceWithInit_Inspect_ReturnNull() {
        PerformanceInspectionResult inspectionResult = PerformanceInspectionService.getInstance()
                .inspect(PerformanceInspection.createAndroidBatteryInfoInspection(
                        "appId", "deviceIdentifier", "custom description"));
        Assertions.assertNull(inspectionResult);
    }
}
