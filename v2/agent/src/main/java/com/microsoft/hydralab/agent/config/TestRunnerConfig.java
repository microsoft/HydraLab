// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.config;

import com.microsoft.hydralab.agent.command.DeviceScriptCommand;
import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.agent.runner.TestRunnerManager;
import com.microsoft.hydralab.agent.runner.analysis.scanner.APKScanner;
import com.microsoft.hydralab.agent.runner.appium.AppiumCrossRunner;
import com.microsoft.hydralab.agent.runner.appium.AppiumRunner;
import com.microsoft.hydralab.agent.runner.espresso.EspressoRunner;
import com.microsoft.hydralab.agent.runner.maestro.MaestroRunner;
import com.microsoft.hydralab.agent.runner.monkey.AdbMonkeyRunner;
import com.microsoft.hydralab.agent.runner.monkey.AppiumMonkeyRunner;
import com.microsoft.hydralab.agent.runner.python.PythonRunner;
import com.microsoft.hydralab.agent.runner.smart.SmartRunner;
import com.microsoft.hydralab.agent.runner.smart.SmartTestUtil;
import com.microsoft.hydralab.agent.runner.xctest.XCTestRunner;
import com.microsoft.hydralab.agent.service.TestTaskEngineService;
import com.microsoft.hydralab.common.entity.agent.LLMProperties;
import com.microsoft.hydralab.common.entity.common.Task;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class TestRunnerConfig {
    @Value("${app.registry.name}")
    String agentName;

    @Value("${app.runner.analysis.enabled:false}")
    boolean isAnalysisEnabled;

    @Bean
    public TestRunnerManager testRunnerManager(AgentManagementService agentManagementService,
                                               TestTaskEngineService testTaskEngineService,
                                               TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                                               PerformanceTestManagementService performanceTestManagementService,
                                               ADBOperateUtil adbOperateUtil,
                                               SmartTestUtil smartTestUtil,
                                               LLMProperties llmProperties) {

        TestRunnerManager testRunnerManager = new TestRunnerManager();

        EspressoRunner espressoRunner = new EspressoRunner(agentManagementService, testTaskEngineService, testRunDeviceOrchestrator, performanceTestManagementService,
                adbOperateUtil);
        testRunnerManager.addRunEngine(Task.RunnerType.INSTRUMENTATION, espressoRunner);

        AppiumRunner appiumRunner = new AppiumRunner(agentManagementService, testTaskEngineService, testRunDeviceOrchestrator, performanceTestManagementService);
        testRunnerManager.addRunEngine(Task.RunnerType.APPIUM, appiumRunner);

        AppiumCrossRunner appiumCrossRunner = new AppiumCrossRunner(agentManagementService, testTaskEngineService,
                testRunDeviceOrchestrator, performanceTestManagementService,
                agentName);
        testRunnerManager.addRunEngine(Task.RunnerType.APPIUM_CROSS, appiumCrossRunner);

        SmartRunner smartRunner = new SmartRunner(agentManagementService, testTaskEngineService, testRunDeviceOrchestrator, performanceTestManagementService,
                smartTestUtil, llmProperties);
        testRunnerManager.addRunEngine(Task.RunnerType.SMART, smartRunner);

        AdbMonkeyRunner adbMonkeyRunner = new AdbMonkeyRunner(agentManagementService, testTaskEngineService, testRunDeviceOrchestrator, performanceTestManagementService,
                adbOperateUtil);
        testRunnerManager.addRunEngine(Task.RunnerType.MONKEY, adbMonkeyRunner);

        AppiumMonkeyRunner appiumMonkeyRunner = new AppiumMonkeyRunner(agentManagementService, testTaskEngineService, testRunDeviceOrchestrator,
                performanceTestManagementService);
        testRunnerManager.addRunEngine(Task.RunnerType.APPIUM_MONKEY, appiumMonkeyRunner);

        XCTestRunner xctestRunner = new XCTestRunner(agentManagementService, testTaskEngineService, testRunDeviceOrchestrator, performanceTestManagementService);
        testRunnerManager.addRunEngine(Task.RunnerType.XCTEST, xctestRunner);

        MaestroRunner maestroRunner = new MaestroRunner(agentManagementService, testTaskEngineService, testRunDeviceOrchestrator, performanceTestManagementService);
        testRunnerManager.addRunEngine(Task.RunnerType.MAESTRO, maestroRunner);

        PythonRunner pythonRunner = new PythonRunner(agentManagementService, testTaskEngineService, testRunDeviceOrchestrator, performanceTestManagementService);
        testRunnerManager.addRunEngine(Task.RunnerType.PYTHON, pythonRunner);

        APKScanner apkScanner = new APKScanner(agentManagementService, testTaskEngineService, isAnalysisEnabled);
        testRunnerManager.addRunEngine(Task.RunnerType.APK_SCANNER, apkScanner);

        return testRunnerManager;
    }

    @Bean
    public PerformanceTestManagementService performanceTestManagementService() {
        PerformanceTestManagementService performanceTestManagementService = new PerformanceTestManagementService();
        performanceTestManagementService.initialize();
        return performanceTestManagementService;
    }

    @ConfigurationProperties(prefix = "app.device-script.commands")
    @Bean(name = "DeviceCommandProperty")
    public List<DeviceScriptCommand> deviceCommandProperty() {
        return new ArrayList<>();
    }
}
