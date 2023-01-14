package com.microsoft.hydralab.performanc;

import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceInspectionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PerformanceInspectionServiceTest {
    @Test
    public void serviceWithInit_Inspect_ReturnNull() {
        PerformanceInspectionResult inspectionResult =
                PerformanceInspectionService.getInstance().inspect(
                        PerformanceInspection.createAndroidBatteryInfoSpec("appId", "deviceIdentifier"));
        Assertions.assertNull(inspectionResult);
    }
}
