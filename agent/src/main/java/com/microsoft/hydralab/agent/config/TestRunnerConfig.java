package com.microsoft.hydralab.agent.config;

import com.microsoft.hydralab.agent.runner.appium.AppiumCrossRunner;
import com.microsoft.hydralab.agent.runner.appium.AppiumRunner;
import com.microsoft.hydralab.agent.runner.espresso.EspressoRunner;
import com.microsoft.hydralab.agent.runner.monkey.AdbMonkeyRunner;
import com.microsoft.hydralab.agent.runner.monkey.AppiumMonkeyRunner;
import com.microsoft.hydralab.agent.runner.smart.SmartRunner;
import com.microsoft.hydralab.agent.runner.t2c.T2CRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestRunnerConfig {

    @Bean
    public EspressoRunner espressoRunner() {
        return new EspressoRunner();
    }

    @Bean
    public AdbMonkeyRunner adbMonkeyRunner() {
        return new AdbMonkeyRunner();
    }

    @Bean
    public AppiumMonkeyRunner appiumMonkeyRunner() {
        return new AppiumMonkeyRunner();
    }

    @Bean
    public AppiumRunner appiumRunner() {
        return new AppiumRunner();
    }

    @Bean
    public AppiumCrossRunner appiumCrossRunner() {
        return new AppiumCrossRunner();
    }

    @Bean
    public SmartRunner smartRunner() {
        return new SmartRunner();
    }

    @Bean
    public T2CRunner t2CRunner() {
        return new T2CRunner();
    }
}
