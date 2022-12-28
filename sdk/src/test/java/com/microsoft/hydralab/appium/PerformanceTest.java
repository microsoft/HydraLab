package com.microsoft.hydralab.appium;

import com.microsoft.hydralab.performance.PerformanceInspectionService;
import com.microsoft.hydralab.performance.PerformanceTestSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static com.microsoft.hydralab.performance.PerformanceTestSpec.*;

/**
 * Sample of performance test. Will not check in
 */
public class PerformanceTest {
    @Test
    public void performanceTestCase() {
        PerformanceInspectionService performanceInspectionService = ThreadParam.getPerformanceExecutor();
        PerformanceTestSpec androidPerfSpec = new PerformanceTestSpec(
                new ArrayList<>(Arrays.asList(TYPE_ANDROID_BATTERY_INFO, TYPE_ANDROID_MEMORY_INFO, TYPE_ANDROID_MEMORY_DUMP)),
                "com.mocrosoft.appmanager",
                "Android", "Initialize");
        PerformanceTestSpec windowsPerfSpec = new PerformanceTestSpec(
                new ArrayList<>(Arrays.asList(TYPE_WIN_BATTERY, TYPE_WIN_MEMORY)),
                "Microsoft.YourPhone_8wekyb3d8bbwe",
                "Windows", "Initialize");
        performanceInspectionService.initialize(androidPerfSpec);
        performanceInspectionService.initialize(windowsPerfSpec);

        //testing...
        System.out.println("Start LTW...");
        androidPerfSpec.setName("Start LTW");
        performanceInspectionService.inspect(androidPerfSpec);
        androidPerfSpec.setAppId("com.mocrosoft.systemapp");
        performanceInspectionService.inspect(androidPerfSpec);

        System.out.println("Start PL...");
        windowsPerfSpec.setName("Start PL");
        performanceInspectionService.inspect(windowsPerfSpec);

    }
}
