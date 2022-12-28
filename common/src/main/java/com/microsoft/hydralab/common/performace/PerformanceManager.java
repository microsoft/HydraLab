package com.microsoft.hydralab.common.performace;

import com.microsoft.hydralab.common.performace.impl.AndroidBatteryInspector;
import com.microsoft.hydralab.common.performace.impl.AndroidMemoryInspector;
import com.microsoft.hydralab.common.performace.impl.WindowsBatteryInspector;
import com.microsoft.hydralab.common.performace.impl.WindowsMemoryInspector;
import org.springframework.stereotype.Service;

@Service
public class PerformanceManager {
    private AndroidBatteryInspector androidBatteryInspector;
    private AndroidMemoryInspector androidMemoryInspector;
    private WindowsBatteryInspector windowsBatteryInspector;
    private WindowsMemoryInspector windowsMemoryInspector;

    PerformanceManager() {
        androidBatteryInspector = new AndroidBatteryInspector();
        androidMemoryInspector = new AndroidMemoryInspector();
        windowsBatteryInspector = new WindowsBatteryInspector();
        windowsMemoryInspector = new WindowsMemoryInspector();
    }


    public AndroidBatteryInspector getAndroidBatteryInspector() {
        return androidBatteryInspector;
    }

    public AndroidMemoryInspector getAndroidMemoryInspector() {
        return androidMemoryInspector;
    }

    public WindowsBatteryInspector getWindowsBatteryInspector() {
        return windowsBatteryInspector;
    }

    public WindowsMemoryInspector getWindowsMemoryInspector() {
        return windowsMemoryInspector;
    }
}
