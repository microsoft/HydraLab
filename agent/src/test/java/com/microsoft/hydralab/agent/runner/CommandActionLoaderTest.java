package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.agent.command.DeviceScriptCommandLoader;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.entity.common.DeviceAction;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRunDeviceCombo;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.device.DeviceType;
import com.microsoft.hydralab.common.management.device.impl.AndroidDeviceDriver;
import com.microsoft.hydralab.common.management.device.impl.DeviceDriverManager;
import com.microsoft.hydralab.common.management.device.impl.WindowsDeviceDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.annotation.Resource;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CommandActionLoaderTest extends BaseTest {

    @Resource
    DeviceScriptCommandLoader commandLoader;

    @Resource
    TestRunDeviceOrchestrator testRunDeviceOrchestrator;

    @MockBean
    DeviceDriverManager deviceDriverManager;

    @Test
    void testAttachCommandAction() {
        TestTask testTask = new TestTask();
        testTask.setTestSuite("com.microsoft.test");
        commandLoader.loadCommandAction(testTask);
        Assertions.assertEquals(testTask.getDeviceActions().size(), 2, "Analysis commands failed!");
        Assertions.assertEquals(testTask.getDeviceActions().get("setUp").size(), 4, "Analysis commands failed!");
        Assertions.assertEquals(testTask.getDeviceActions().get("tearDown").size(), 3, "Analysis commands failed!");

        AndroidDeviceDriver androidDeviceDriver = Mockito.mock(AndroidDeviceDriver.class);
        WindowsDeviceDriver windowsDeviceDriver = Mockito.mock(WindowsDeviceDriver.class);
        deviceDriverManager.addDeviceDriver(DeviceType.ANDROID, androidDeviceDriver);
        deviceDriverManager.addDeviceDriver(DeviceType.WINDOWS, windowsDeviceDriver);

        ActionExecutor actionExecutor = new ActionExecutor();
        DeviceInfo androidDevice = new DeviceInfo();
        androidDevice.setType(DeviceType.ANDROID.name());

        DeviceInfo windowsDevice = new DeviceInfo();
        windowsDevice.setType(DeviceType.WINDOWS.name());

        TestRunDeviceCombo testRunDeviceCombo = new TestRunDeviceCombo(androidDevice, List.of(windowsDevice));

        testRunDeviceOrchestrator.doActions(testRunDeviceCombo, baseLogger, testTask.getDeviceActions(), DeviceAction.When.SET_UP);
        verify(deviceDriverManager, times(3)).execCommandOnDevice(Mockito.any(DeviceInfo.class), Mockito.anyString(),
                Mockito.any(Logger.class));
        verify(deviceDriverManager, times(1)).execCommandOnAgent(Mockito.any(DeviceInfo.class), Mockito.anyString(),
                Mockito.any(Logger.class));

        testRunDeviceOrchestrator.doActions(testRunDeviceCombo, baseLogger, testTask.getDeviceActions(), DeviceAction.When.TEAR_DOWN);
        verify(deviceDriverManager, times(4)).execCommandOnDevice(Mockito.any(DeviceInfo.class), Mockito.anyString(),
                Mockito.any(Logger.class));
        verify(deviceDriverManager, times(3)).execCommandOnAgent(Mockito.any(DeviceInfo.class), Mockito.anyString(),
                Mockito.any(Logger.class));

    }
}