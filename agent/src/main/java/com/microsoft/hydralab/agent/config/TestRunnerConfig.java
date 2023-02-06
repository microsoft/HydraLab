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
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
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
    public EspressoRunner espressoRunner(DeviceManager deviceManager, TestTaskEngineService testTaskEngineService, ADBOperateUtil adbOperateUtil) {
        return new EspressoRunner(deviceManager, testTaskEngineService, adbOperateUtil);
    }

    @Bean
    public AdbMonkeyRunner adbMonkeyRunner(DeviceManager deviceManager, TestTaskEngineService testTaskEngineService, ADBOperateUtil adbOperateUtil) {
        return new AdbMonkeyRunner(deviceManager, testTaskEngineService, adbOperateUtil);
    }

    @Bean
    public AppiumMonkeyRunner appiumMonkeyRunner(DeviceManager deviceManager, TestTaskEngineService testTaskEngineService) {
        return new AppiumMonkeyRunner(deviceManager, testTaskEngineService);
    }

    @Bean
    public AppiumRunner appiumRunner(DeviceManager deviceManager, TestTaskEngineService testTaskEngineService) {
        return new AppiumRunner(deviceManager, testTaskEngineService);
    }

    @Bean
    public AppiumCrossRunner appiumCrossRunner(DeviceManager deviceManager, TestTaskEngineService testTaskEngineService) {
        return new AppiumCrossRunner(deviceManager, testTaskEngineService, agentName);
    }

    @Bean
    public SmartRunner smartRunner(DeviceManager deviceManager, TestTaskEngineService testTaskEngineService, SmartTestUtil smartTestUtil) {
        return new SmartRunner(deviceManager, testTaskEngineService, smartTestUtil);
    }

    @Bean
    public T2CRunner t2cRunner(DeviceManager deviceManager, TestTaskEngineService testTaskEngineService) {
        return new T2CRunner(deviceManager, testTaskEngineService, agentName);
    }

    @ConfigurationProperties(prefix = "app.device-script.commands")
    @Bean(name = "DeviceCommandProperty")
    public List<DeviceScriptCommand> DeviceCommandProperty() {
        return new ArrayList<>();
    }
}
