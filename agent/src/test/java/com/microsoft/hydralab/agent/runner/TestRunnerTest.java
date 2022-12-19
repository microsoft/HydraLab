package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.agent.service.TestTaskEngineService;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.DeviceManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.File;

public class TestRunnerTest extends BaseTest {
    @Resource
    DeviceManager deviceManager;
    @Resource
    TestTaskEngineService testTaskEngineService;
    final Logger logger = LoggerFactory.getLogger(TestRunnerTest.class);

    @Test
    public void createTestRunnerAndInitDeviceTest() {
        TestRunner testRunner = new TestRunner(deviceManager, testTaskEngineService) {
            @Override
            protected void run(DeviceInfo deviceInfo, TestTask testTask, DeviceTestTask deviceTestTask) throws Exception {

            }
        };

        DeviceInfo deviceInfo = Mockito.mock(DeviceInfo.class);
        Mockito.when(deviceInfo.getSerialNum()).thenReturn("build");
        Mockito.when(deviceInfo.getName()).thenReturn("Name");

        File resourceDir = new File("").getAbsoluteFile();
        logger.info("resourceDir path is {}", resourceDir.getAbsolutePath());

        TestTask testTask = new TestTask();
        testTask.setResourceDir(resourceDir);
        testTask.setTestSuite("TestSuite");

        DeviceTestTask deviceTestTask = testRunner.buildDeviceTestTask(deviceInfo, testTask, logger);

        deviceTestTask.getLogger().info("Test DeviceTestTask logging function");
        deviceTestTask.getLogger().info("DeviceTestTask InstrumentReportPath {}", deviceTestTask.getInstrumentReportPath());

        Assertions.assertTrue(new File(deviceTestTask.getInstrumentReportPath()).exists());
    }
}
