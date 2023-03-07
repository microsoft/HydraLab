package com.microsoft.hydralab.agent.service;

import com.microsoft.hydralab.agent.config.TestRunnerConfig;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.espresso.EspressoRunner;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.entity.common.TestTaskSpec;
import com.microsoft.hydralab.common.entity.common.TestFileSet;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.device.TestDeviceManager;
import com.microsoft.hydralab.common.management.device.impl.AndroidTestDeviceManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;

public class TestTaskEngineServiceTest extends BaseTest {

    @Resource
    TestTaskEngineService testTaskEngineService;
    @Resource
    TestDeviceManager testDeviceManager;
    @MockBean
    EspressoRunner espressoRunner;
    @Resource
    ApplicationContext applicationContext;

    @Test
    public void runTestTask() {
        TestTaskSpec taskSpecForGroupDevice = new TestTaskSpec();
        taskSpecForGroupDevice.runningType = TestTask.TestRunningType.INSTRUMENTATION;
        taskSpecForGroupDevice.deviceIdentifier = "G.UnitTest";
        taskSpecForGroupDevice.testFileSet = new TestFileSet();
        taskSpecForGroupDevice.groupDevices = "TestDeviceSerial1,TestDeviceSerial2";
        taskSpecForGroupDevice.pkgName = "com.microsoft.test";

        String beanName = TestRunnerConfig.testRunnerMap.get(taskSpecForGroupDevice.runningType);
        TestRunner runner = applicationContext.getBean(beanName, TestRunner.class);
        baseLogger.info("Try to get bean by name: " + taskSpecForGroupDevice);

        Assertions.assertTrue(runner instanceof EspressoRunner, "Get runner bean error!");

        testTaskEngineService.runTestTask(taskSpecForGroupDevice);

        TestTaskSpec taskSpecForSingleDevice = new TestTaskSpec();
        taskSpecForSingleDevice.runningType = TestTask.TestRunningType.INSTRUMENTATION;
        taskSpecForSingleDevice.deviceIdentifier = "TestDeviceSerial1";
        taskSpecForSingleDevice.testFileSet = new TestFileSet();
        taskSpecForSingleDevice.pkgName = "com.microsoft.test";
        testTaskEngineService.runTestTask(taskSpecForSingleDevice);
    }

    @Test
    public void getDeviceManager() {
        baseLogger.info(String.valueOf(testDeviceManager instanceof AndroidTestDeviceManager));
        Assertions.assertTrue(testDeviceManager instanceof AndroidTestDeviceManager, "Init DeviceManager Bean Error!");
    }
}