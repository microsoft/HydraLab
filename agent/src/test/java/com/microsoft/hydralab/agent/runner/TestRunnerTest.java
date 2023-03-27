package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.agent.runner.espresso.EspressoRunner;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.device.TestDevice;
import com.microsoft.hydralab.common.util.Const;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.File;

public class TestRunnerTest extends BaseTest {
    @Resource
    EspressoRunner espressoRunner;
    final Logger logger = LoggerFactory.getLogger(TestRunnerTest.class);

    @Test
    public void createTestRunnerAndInitDeviceTest() {

        DeviceInfo deviceInfo = Mockito.mock(DeviceInfo.class);
        Mockito.when(deviceInfo.getSerialNum()).thenReturn("build");
        Mockito.when(deviceInfo.getName()).thenReturn("Name");

        File resourceDir = new File("").getAbsoluteFile();
        logger.info("resourceDir path is {}", resourceDir.getAbsolutePath());

        TestTask testTask = new TestTask();
        testTask.setResourceDir(resourceDir);
        testTask.setTestSuite("TestSuite");

        TestRun testRun = espressoRunner.createTestRun(new TestDevice(deviceInfo, Const.TestDeviceTag.PRIMARY_PHONE), testTask, logger);

        testRun.getLogger().info("Test TestRun logging function");
        testRun.getLogger().info("TestRun InstrumentReportPath {}", testRun.getInstrumentReportPath());

        Assertions.assertTrue(new File(testRun.getInstrumentReportPath()).exists());
    }
}
