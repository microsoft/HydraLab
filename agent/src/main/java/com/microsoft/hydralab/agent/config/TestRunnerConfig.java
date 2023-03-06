// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.config;

import com.microsoft.hydralab.agent.command.DeviceScriptCommand;
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
import com.microsoft.hydralab.common.management.device.TestDeviceManager;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class TestRunnerConfig {
    @Value("${app.registry.name}")
    String agentName;
    @SuppressWarnings("visibilitymodifier")
    public static Map<String, String> testRunnerMap = Map.of(
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
    public EspressoRunner espressoRunner(TestDeviceManager testDeviceManager, TestTaskEngineService testTaskEngineService,
                                         PerformanceTestManagementService performanceTestManagementService,
                                         ADBOperateUtil adbOperateUtil) {
        return new EspressoRunner(testDeviceManager, testTaskEngineService, performanceTestManagementService,
                adbOperateUtil);
    }

    @Bean
    public AdbMonkeyRunner adbMonkeyRunner(TestDeviceManager testDeviceManager, TestTaskEngineService testTaskEngineService,
                                           PerformanceTestManagementService performanceTestManagementService,
                                           ADBOperateUtil adbOperateUtil) {
        return new AdbMonkeyRunner(testDeviceManager, testTaskEngineService, performanceTestManagementService,
                adbOperateUtil);
    }

    @Bean
    public AppiumMonkeyRunner appiumMonkeyRunner(TestDeviceManager testDeviceManager,
                                                 TestTaskEngineService testTaskEngineService,
                                                 PerformanceTestManagementService performanceTestManagementService) {
        return new AppiumMonkeyRunner(testDeviceManager, testTaskEngineService, performanceTestManagementService);
    }

    @Bean
    public AppiumRunner appiumRunner(TestDeviceManager testDeviceManager, TestTaskEngineService testTaskEngineService,
                                     PerformanceTestManagementService performanceTestManagementService) {
        return new AppiumRunner(testDeviceManager, testTaskEngineService, performanceTestManagementService);
    }

    @Bean
    public AppiumCrossRunner appiumCrossRunner(TestDeviceManager testDeviceManager,
                                               TestTaskEngineService testTaskEngineService,
                                               PerformanceTestManagementService performanceTestManagementService) {
        return new AppiumCrossRunner(testDeviceManager, testTaskEngineService, performanceTestManagementService,
                agentName);
    }

    @Bean
    public SmartRunner smartRunner(TestDeviceManager testDeviceManager, TestTaskEngineService testTaskEngineService,
                                   PerformanceTestManagementService performanceTestManagementService,
                                   SmartTestUtil smartTestUtil) {
        return new SmartRunner(testDeviceManager, testTaskEngineService, performanceTestManagementService,
                smartTestUtil);
    }

    @Bean
    public T2CRunner t2cRunner(TestDeviceManager testDeviceManager, TestTaskEngineService testTaskEngineService,
                               PerformanceTestManagementService performanceTestManagementService) {
        return new T2CRunner(testDeviceManager, testTaskEngineService, performanceTestManagementService, agentName);
    }

    @ConfigurationProperties(prefix = "app.device-script.commands")
    @Bean(name = "DeviceCommandProperty")
    public List<DeviceScriptCommand> deviceCommandProperty() {
        return new ArrayList<>();
    }
}
