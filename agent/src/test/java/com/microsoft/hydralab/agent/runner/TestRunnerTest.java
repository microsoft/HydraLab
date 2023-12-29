package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.agent.runner.espresso.EspressoRunner;
import com.microsoft.hydralab.agent.service.TestTaskEngineService;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.Task;
import com.microsoft.hydralab.common.entity.common.TestFileSet;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.entity.common.TestTaskSpec;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.device.DeviceType;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.File;

public class TestRunnerTest extends BaseTest {
    final Logger logger = LoggerFactory.getLogger(TestRunnerTest.class);

    @Resource
    TestRunDeviceOrchestrator testRunDeviceOrchestrator;
    @Resource
    AgentManagementService agentManagementService;
    @Resource
    TestTaskEngineService testTaskEngineService;
    @Resource
    PerformanceTestManagementService performanceTestManagementService;
    @Resource
    ADBOperateUtil adbOperateUtil;

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
        EspressoRunner espressoRunner = new EspressoRunner(agentManagementService, testTaskEngineService, testRunDeviceOrchestrator, performanceTestManagementService,
                adbOperateUtil);
        TestRunDevice testRunDevice = new TestRunDevice(deviceInfo, deviceInfo.getType());
        testRunDevice.setLogger(logger);
        TestRun testRun = espressoRunner.initTestRun(testTask, testRunDevice);

        testRun.getLogger().info("Test TestRun logging function");
        testRun.getLogger().info("TestRun InstrumentReportPath {}", testRun.getInstrumentReportPath());

        Assertions.assertTrue(new File(testRun.getInstrumentReportPath()).exists());
    }

    @Test
    public void testTestRunnerRun() {
        TestTaskSpec taskSpecForGroupDevice = new TestTaskSpec();
        taskSpecForGroupDevice.runningType = Task.RunnerType.APPIUM_CROSS.name();
        taskSpecForGroupDevice.deviceIdentifier = "TestDeviceSerial1,TestDeviceSerial2";
        taskSpecForGroupDevice.testFileSet = new TestFileSet();

        TestRunnerManager testRunnerManager = new TestRunnerManager();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            testRunnerManager.runTestTask(new TestTask(taskSpecForGroupDevice), null);
        }, "Should throw IllegalArgumentException when there is no runner for the test task");

    }
}
