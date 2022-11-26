package com.microsoft.hydralab.agent.service;

import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.espresso.EspressoRunner;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.entity.center.TestTaskSpec;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.management.impl.AndroidDeviceManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;

public class TestTaskEngineServiceTest extends BaseTest {

    @Resource
    TestTaskEngineService testTaskEngineService;
    @Resource
    DeviceManager deviceManager;
    @MockBean
    EspressoRunner espressoRunner;
    @Resource
    ApplicationContext applicationContext;

    @Test
    public void runTestTask() {
        TestTaskSpec taskSpec = new TestTaskSpec();
        taskSpec.runningType = TestTask.TestRunningType.INSTRUMENTATION;
        String beanName = TestTask.TestRunnerMap.get(taskSpec.runningType);
        TestRunner runner = applicationContext.getBean(beanName, TestRunner.class);
        baseLogger.info("Try to get bean by name: " + taskSpec);
        Assertions.assertTrue(runner instanceof EspressoRunner, "Get runner bean error!");
        testTaskEngineService.runTestTask(taskSpec);
    }

    @Test
    public void getDeviceManager() {
        baseLogger.info(String.valueOf(deviceManager instanceof AndroidDeviceManager));
        Assertions.assertTrue(deviceManager instanceof AndroidDeviceManager, "Init DeviceManager Bean Error!");
    }
}