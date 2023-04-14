package com.microsoft.hydralab.agent.service;

import com.microsoft.hydralab.agent.config.TestRunnerConfig;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.espresso.EspressoRunner;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestFileSet;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestRunDeviceCombo;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.entity.common.TestTaskSpec;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.device.DeviceType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

public class TestTaskEngineServiceTest extends BaseTest {

    @Resource
    TestTaskEngineService testTaskEngineService;
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

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            testTaskEngineService.runTestTask(TestTask.convertToTestTask(taskSpecForGroupDevice));
        }, "Should throw IllegalArgumentException when deviceIdentifier is not exist");

        TestTaskSpec taskSpecForSingleDevice = new TestTaskSpec();
        taskSpecForSingleDevice.runningType = TestTask.TestRunningType.INSTRUMENTATION;
        taskSpecForSingleDevice.deviceIdentifier = "TestDeviceSerial1";
        taskSpecForSingleDevice.testFileSet = new TestFileSet();
        taskSpecForSingleDevice.pkgName = "com.microsoft.test";
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            testTaskEngineService.runTestTask(TestTask.convertToTestTask(taskSpecForSingleDevice));
        }, "Should throw IllegalArgumentException when deviceIdentifier is not exist");
    }

    @Test
    public void testChooseDevices() {
        Set<DeviceInfo> deviceInfoList = new HashSet<>();
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setSerialNum("TestDeviceSerial1");
        deviceInfo.setType(DeviceType.ANDROID.name());
        deviceInfoList.add(deviceInfo);
        deviceInfo = new DeviceInfo();
        deviceInfo.setSerialNum("TestDeviceSerial2");
        deviceInfo.setType(DeviceType.WINDOWS.name());
        deviceInfoList.add(deviceInfo);
        deviceInfo = new DeviceInfo();
        deviceInfo.setSerialNum("TestDeviceSerial3");
        deviceInfo.setType(DeviceType.ANDROID.name());
        deviceInfoList.add(deviceInfo);
        deviceInfo = new DeviceInfo();
        deviceInfo.setSerialNum("TestDeviceSerial4");
        deviceInfo.setType(DeviceType.ANDROID.name());
        deviceInfoList.add(deviceInfo);
        deviceInfo = new DeviceInfo();
        deviceInfo.setSerialNum("TestDeviceSerial5");
        deviceInfo.setType(DeviceType.IOS.name());
        deviceInfoList.add(deviceInfo);

        AgentManagementService mockAgentMgmService = Mockito.mock(AgentManagementService.class);
        testTaskEngineService.agentManagementService = mockAgentMgmService;
        Mockito.when(mockAgentMgmService.getActiveDeviceList(TestTaskEngineService.log)).thenReturn(deviceInfoList);

        TestTaskSpec taskSpecForGroupDevice = new TestTaskSpec();
        taskSpecForGroupDevice.runningType = TestTask.TestRunningType.APPIUM_CROSS;
        taskSpecForGroupDevice.deviceIdentifier = "G.UnitTest";
        taskSpecForGroupDevice.groupDevices = "TestDeviceSerial1,TestDeviceSerial2";
        taskSpecForGroupDevice.testFileSet = new TestFileSet();
        Set<TestRunDevice> testRunDevices = testTaskEngineService.chooseDevices(TestTask.convertToTestTask(taskSpecForGroupDevice));
        Assertions.assertEquals(1, testRunDevices.size(), "Should get 1 devices for cross device group device");
        Assertions.assertTrue(testRunDevices.iterator().next() instanceof TestRunDeviceCombo, "Should get TestRunDeviceCombo for cross device group device");

        taskSpecForGroupDevice = new TestTaskSpec();
        taskSpecForGroupDevice.runningType = TestTask.TestRunningType.INSTRUMENTATION;
        taskSpecForGroupDevice.deviceIdentifier = "G.UnitTest";
        taskSpecForGroupDevice.groupDevices = "TestDeviceSerial1,TestDeviceSerial3,TestDeviceSerial4,TestDeviceSerialX";
        taskSpecForGroupDevice.testFileSet = new TestFileSet();
        testRunDevices = testTaskEngineService.chooseDevices(TestTask.convertToTestTask(taskSpecForGroupDevice));
        Assertions.assertEquals(3, testRunDevices.size(), "Should get 3 devices for cross device group device");

        TestTaskSpec taskSpecForSingleDevice = new TestTaskSpec();
        taskSpecForSingleDevice.runningType = TestTask.TestRunningType.INSTRUMENTATION;
        taskSpecForSingleDevice.deviceIdentifier = "TestDeviceSerial1";
        taskSpecForSingleDevice.testFileSet = new TestFileSet();
        testRunDevices = testTaskEngineService.chooseDevices(TestTask.convertToTestTask(taskSpecForSingleDevice));
        Assertions.assertEquals(1, testRunDevices.size(), "Should get 1 devices for cross device group device");

        taskSpecForSingleDevice = new TestTaskSpec();
        taskSpecForSingleDevice.runningType = TestTask.TestRunningType.INSTRUMENTATION;
        taskSpecForSingleDevice.deviceIdentifier = "TestDeviceSerialX";
        taskSpecForSingleDevice.testFileSet = new TestFileSet();
        testRunDevices = testTaskEngineService.chooseDevices(TestTask.convertToTestTask(taskSpecForSingleDevice));
        Assertions.assertEquals(0, testRunDevices.size(), "Should get no devices for cross device group device");
    }
}