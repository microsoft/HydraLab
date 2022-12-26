package com.microsoft.hydralab.common.performace;

import com.microsoft.hydralab.common.performace.impl.AndroidBatteryInspector;
import com.microsoft.hydralab.common.performace.impl.AndroidMemInspector;

public class PerformanceManager {
    private AndroidBatteryInspector androidBatteryInspector;
    private AndroidMemInspector androidMemInspector;

    PerformanceManager() {
        androidBatteryInspector = new AndroidBatteryInspector();
        androidMemInspector = new AndroidMemInspector();
    }
}
