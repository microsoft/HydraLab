// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.config;

import com.microsoft.hydralab.agent.runner.appium.AppiumCrossRunner;
import com.microsoft.hydralab.agent.runner.appium.AppiumRunner;
import com.microsoft.hydralab.agent.runner.espresso.EspressoRunner;
import com.microsoft.hydralab.agent.runner.monkey.AdbMonkeyRunner;
import com.microsoft.hydralab.agent.runner.monkey.AppiumMonkeyRunner;
import com.microsoft.hydralab.agent.runner.smart.SmartRunner;
import com.microsoft.hydralab.agent.runner.smart.SmartTestUtil;
import com.microsoft.hydralab.agent.runner.t2c.T2CRunner;
import com.microsoft.hydralab.agent.service.TestTaskEngineService;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class TestRunnerConfig {
    @Value("${app.registry.name}")
    String agentName;
    public static Map<String, String> TestRunnerMap = Map.of(
            TestTask.TestRunningType.INSTRUMENTATION, "espressoRunner",
            TestTask.TestRunningType.APPIUM, "appiumRunner",
            TestTask.TestRunningType.APPIUM_CROSS, "appiumCrossRunner",
            TestTask.TestRunningType.SMART_TEST, "smartRunner",
            TestTask.TestRunningType.MONKEY_TEST, "adbMonkeyRunner",
            TestTask.TestRunningType.APPIUM_MONKEY_TEST, "appiumMonkeyRunner",
            TestTask.TestRunningType.T2C_JSON_TEST, "t2cRunner"
    );
    @Bean
    public PerformanceTestManagementService performanceTestManagementService() {
        PerformanceTestManagementService performanceTestManagementService = new PerformanceTestManagementService();
        performanceTestManagementService.initialize();
        return performanceTestManagementService;
    }

    @Bean
    public EspressoRunner espressoRunner(DeviceManager deviceManager, TestTaskEngineService testTaskEngineService, ADBOperateUtil adbOperateUtil, PerformanceTestManagementService performanceTestManagementService) {
        return new EspressoRunner(deviceManager, testTaskEngineService, adbOperateUtil, performanceTestManagementService);
    }

    @Bean
    public AdbMonkeyRunner adbMonkeyRunner(DeviceManager deviceManager, TestTaskEngineService testTaskEngineService, ADBOperateUtil adbOperateUtil, PerformanceTestManagementService performanceTestManagementService) {
        return new AdbMonkeyRunner(deviceManager, testTaskEngineService, adbOperateUtil, performanceTestManagementService);
    }

    @Bean
    public AppiumMonkeyRunner appiumMonkeyRunner(DeviceManager deviceManager, TestTaskEngineService testTaskEngineService, PerformanceTestManagementService performanceTestManagementService) {
        return new AppiumMonkeyRunner(deviceManager, testTaskEngineService, performanceTestManagementService);
    }

    @Bean
    public AppiumRunner appiumRunner(DeviceManager deviceManager, TestTaskEngineService testTaskEngineService, PerformanceTestManagementService performanceTestManagementService) {
        return new AppiumRunner(deviceManager, testTaskEngineService, performanceTestManagementService);
    }

    @Bean
    public AppiumCrossRunner appiumCrossRunner(DeviceManager deviceManager, TestTaskEngineService testTaskEngineService, PerformanceTestManagementService performanceTestManagementService) {
        return new AppiumCrossRunner(deviceManager, testTaskEngineService, agentName, performanceTestManagementService);
    }

    @Bean
    public SmartRunner smartRunner(DeviceManager deviceManager, TestTaskEngineService testTaskEngineService, SmartTestUtil smartTestUtil, PerformanceTestManagementService performanceTestManagementService) {
        return new SmartRunner(deviceManager, testTaskEngineService, smartTestUtil, performanceTestManagementService);
    }

    @Bean
    public T2CRunner t2cRunner(DeviceManager deviceManager, TestTaskEngineService testTaskEngineService, PerformanceTestManagementService performanceTestManagementService) {
        return new T2CRunner(deviceManager, testTaskEngineService, agentName, performanceTestManagementService);
    }
}
