package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.agent.runner.espresso.EspressoRunner;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestFileSet;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.entity.common.TestTaskSpec;
import com.microsoft.hydralab.common.management.device.DeviceType;
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
        Mockito.when(deviceInfo.getType()).thenReturn(DeviceType.ANDROID.name());

        File resourceDir = new File("").getAbsoluteFile();
        logger.info("resourceDir path is {}", resourceDir.getAbsolutePath());

        TestTask testTask = new TestTask();
        testTask.setResourceDir(resourceDir);
        testTask.setTestSuite("TestSuite");

        TestRunDevice testRunDevice = new TestRunDevice(deviceInfo, deviceInfo.getType());
        testRunDevice.setLogger(logger);
        TestRun testRun = espressoRunner.createTestRun(testRunDevice, testTask);

        testRun.getLogger().info("Test TestRun logging function");
        testRun.getLogger().info("TestRun InstrumentReportPath {}", testRun.getInstrumentReportPath());

        Assertions.assertTrue(new File(testRun.getInstrumentReportPath()).exists());
    }

    @Test
    public void testTestRunnerRun() {
        TestTaskSpec taskSpecForGroupDevice = new TestTaskSpec();
        taskSpecForGroupDevice.runningType = TestTask.TestRunningType.APPIUM_CROSS;
        taskSpecForGroupDevice.deviceIdentifier = "TestDeviceSerial1,TestDeviceSerial2";
        taskSpecForGroupDevice.testFileSet = new TestFileSet();

        TestRunnerManager testRunnerManager = new TestRunnerManager();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            testRunnerManager.runTestTask(TestTask.convertToTestTask(taskSpecForGroupDevice), null);
        }, "Should throw IllegalArgumentException when there is no runner for the test task");

    }
}
