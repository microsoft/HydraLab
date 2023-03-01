// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.entity.common.DeviceAction;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.impl.AndroidDeviceManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ActionExecutorTest extends BaseTest {

    ActionExecutor actionExecutor = new ActionExecutor();

    @Test
    void createAndExecuteActions() throws InvocationTargetException, IllegalAccessException {
        AndroidDeviceManager deviceManager = Mockito.mock(AndroidDeviceManager.class);
        DeviceInfo deviceInfo = new DeviceInfo();
        JSONObject actionJson = new JSONObject();
        actionJson.put("method", "setProperty");
        actionJson.put("deviceType", "Android");
        DeviceAction action1 = JSONObject.parseObject(actionJson.toJSONString(), DeviceAction.class);
        List<String> args1 = List.of("paramA", "paramB");
        action1.setArgs(args1);
        actionExecutor.doAction(deviceManager, deviceInfo, baseLogger, action1);
        verify(deviceManager).setProperty(deviceInfo, args1.get(0), args1.get(1), baseLogger);

        DeviceAction action2 = new DeviceAction("Android", "changeGlobalSetting");
        List<String> args2 = List.of("paramC", "paramD");
        action2.setArgs(args2);
        List<DeviceAction> actions = new ArrayList<>();
        actions.add(action1);
        actions.add(action2);
        List<Exception> exceptions = actionExecutor.doActions(deviceManager, deviceInfo, baseLogger,
                Map.of(DeviceAction.When.SET_UP, actions), DeviceAction.When.SET_UP);
        Assertions.assertTrue(exceptions.size() == 0, () -> exceptions.get(0).getMessage());
        verify(deviceManager, times(2)).setProperty(deviceInfo, args1.get(0), args1.get(1), baseLogger);
        verify(deviceManager, times(1)).changeGlobalSetting(deviceInfo, args2.get(0), args2.get(1), baseLogger);
    }

}