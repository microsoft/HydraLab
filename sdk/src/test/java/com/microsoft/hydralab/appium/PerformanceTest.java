package com.microsoft.hydralab.appium;

import com.microsoft.hydralab.performance.PerformanceExecutor;
import com.microsoft.hydralab.performance.PerformanceTestSpec;
import org.junit.jupiter.api.Test;

public class PerformanceTest {
    @Test
    public void performanceTestCase() {
        PerformanceExecutor performanceExecutor = ThreadParam.getPerformanceExecutor();
        PerformanceTestSpec androidPerfSpec = new PerformanceTestSpec(
                PerformanceTestSpec.FLAG_MEM | PerformanceTestSpec.FLAG_BATTERY,
                "com.mocrosoft.appmanager",
                "Android", "Initialize");
        PerformanceTestSpec windowsPerfSpec = new PerformanceTestSpec(
                PerformanceTestSpec.FLAG_MEM | PerformanceTestSpec.FLAG_BATTERY,
                "Microsoft.YourPhone_8wekyb3d8bbwe",
                "Windows", "Initialize");
        performanceExecutor.initDevice(androidPerfSpec);
        performanceExecutor.initDevice(windowsPerfSpec);

        //testing...
        System.out.println("Start LTW...");
        androidPerfSpec.setName("Start LTW");
        performanceExecutor.capturePerformanceMetrics(androidPerfSpec);
        androidPerfSpec.setAppId("com.mocrosoft.systemapp");
        performanceExecutor.capturePerformanceMetrics(androidPerfSpec);

        System.out.println("Start PL...");
        windowsPerfSpec.setName("Start PL");
        performanceExecutor.capturePerformanceMetrics(windowsPerfSpec);

    }
}
