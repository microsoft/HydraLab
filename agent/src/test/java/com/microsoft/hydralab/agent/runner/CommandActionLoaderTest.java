package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.agent.command.DeviceScriptCommandLoader;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.entity.common.DeviceAction;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.device.DeviceType;
import com.microsoft.hydralab.common.management.device.impl.AndroidDeviceDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import javax.annotation.Resource;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CommandActionLoaderTest extends BaseTest {

    @Resource
    DeviceScriptCommandLoader commandLoader;

    @Test
    void testAttachCommandAction() {
        TestTask testTask = new TestTask();
        testTask.setTestSuite("com.microsoft.test");
        commandLoader.loadCommandAction(testTask);
        Assertions.assertEquals(testTask.getDeviceActions().size(), 2, "Analysis commands failed!");
        Assertions.assertEquals(testTask.getDeviceActions().get("setUp").size(), 4, "Analysis commands failed!");
        Assertions.assertEquals(testTask.getDeviceActions().get("tearDown").size(), 3, "Analysis commands failed!");

        AndroidDeviceDriver deviceDriver = Mockito.mock(AndroidDeviceDriver.class);
        ActionExecutor actionExecutor = new ActionExecutor();
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setType(DeviceType.ANDROID.name());

        actionExecutor.doActions(deviceDriver, new TestRunDevice(deviceInfo, deviceInfo.getType()), baseLogger, testTask.getDeviceActions(),
                DeviceAction.When.SET_UP);
        verify(deviceDriver, times(3)).execCommandOnDevice(Mockito.any(DeviceInfo.class), Mockito.anyString(),
                Mockito.any(Logger.class));
        verify(deviceDriver, times(1)).execCommandOnAgent(Mockito.any(DeviceInfo.class), Mockito.anyString(),
                Mockito.any(Logger.class));

        actionExecutor.doActions(deviceDriver, new TestRunDevice(deviceInfo, deviceInfo.getType()), baseLogger, testTask.getDeviceActions(),
                DeviceAction.When.TEAR_DOWN);
        verify(deviceDriver, times(4)).execCommandOnDevice(Mockito.any(DeviceInfo.class), Mockito.anyString(),
                Mockito.any(Logger.class));
        verify(deviceDriver, times(3)).execCommandOnAgent(Mockito.any(DeviceInfo.class), Mockito.anyString(),
                Mockito.any(Logger.class));

    }
}