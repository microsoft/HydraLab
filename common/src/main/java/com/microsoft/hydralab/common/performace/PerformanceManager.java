package com.microsoft.hydralab.common.performace;

import com.microsoft.hydralab.common.performace.impl.*;
import org.springframework.stereotype.Service;

@Service
public class PerformanceManager {
    private AndroidBatteryInspector androidBatteryInspector;
    private AndroidMemoryDumpInspector androidMemoryDumpInspector;
    private AndroidMemoryInfoInspector androidMemoryInfoInspector;
    private WindowsBatteryInspector windowsBatteryInspector;
    private WindowsMemoryInspector windowsMemoryInspector;

    PerformanceManager() {
        androidBatteryInspector = new AndroidBatteryInspector();
        androidMemoryDumpInspector = new AndroidMemoryDumpInspector();
        androidMemoryInfoInspector = new AndroidMemoryInfoInspector();
        windowsBatteryInspector = new WindowsBatteryInspector();
        windowsMemoryInspector = new WindowsMemoryInspector();
    }

    public AndroidBatteryInspector getAndroidBatteryInspector() {
        return androidBatteryInspector;
    }

    public AndroidMemoryDumpInspector getAndroidMemoryDumpInspector() {
        return androidMemoryDumpInspector;
    }

    public AndroidMemoryInfoInspector getAndroidMemoryInfoInspector() {
        return androidMemoryInfoInspector;
    }

    public WindowsBatteryInspector getWindowsBatteryInspector() {
        return windowsBatteryInspector;
    }

    public WindowsMemoryInspector getWindowsMemoryInspector() {
        return windowsMemoryInspector;
    }
}
