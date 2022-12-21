// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.entity.common.DeviceAction;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.Message;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.util.SerializeUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ActionExecutorTest extends BaseTest {

    ActionExecutor actionExecutor = new ActionExecutor();

    @Test
    void doAction() {
        DeviceManager deviceManager = new MockDeviceManager();
        DeviceInfo deviceInfo = new DeviceInfo();
        JSONObject actionJson = new JSONObject();
        actionJson.put("method", "setProperty");
        actionJson.put("deviceType", "Android");

        DeviceAction action1 = JSONObject.parseObject(actionJson.toJSONString(), DeviceAction.class);
        action1.getArgs().add("paramA");
        action1.getArgs().add("paramB");
        actionExecutor.doAction(deviceManager, deviceInfo, baseLogger, action1);

        DeviceAction action2 = new DeviceAction("Android", "changeGlobalSetting");
        action2.getArgs().add("paramC");
        action2.getArgs().add("paramD");
        List<DeviceAction> actions = new ArrayList<>();
        actions.add(action1);
        actions.add(action2);
        actionExecutor.doActions(deviceManager, deviceInfo, baseLogger, Map.of(DeviceAction.When.SET_UP, actions), DeviceAction.When.SET_UP);

    }

    @Test
    void testJsonArraySerialize() {
        JSONArray array = new JSONArray();
        array.add(1);
        array.add(3);
        array.add(5);
        array.add(2);
        array.add(4);
        array.add(6);
        String str1 = array.toJSONString();
        baseLogger.info(str1);
        Message msg = new Message();
        msg.setBody(array);
        Message msg2 = SerializeUtil.byteArrToMessage(SerializeUtil.messageToByteArr(msg));
        String str2 = ((JSONArray) msg2.getBody()).toJSONString();
        baseLogger.info(str2);
        JSONArray array2 = JSONArray.parseArray(str2);
        String str3 = array2.toJSONString();
        baseLogger.info(str3);
        Assertions.assertEquals(str1, str2, "Serialize error!");
        Assertions.assertEquals(str1, str3, "Serialize error!");

    }

}