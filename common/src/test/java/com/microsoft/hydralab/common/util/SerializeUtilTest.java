package com.microsoft.hydralab.common.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.common.Message;
import com.microsoft.hydralab.common.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;


public class SerializeUtilTest extends BaseTest {

    @Test
    public void messageToByteArr() {
        JSONObject data = new JSONObject();
        Message message = new Message();
        data.put(Const.AgentConfig.task_id_param, UUID.randomUUID().toString());
        message.setPath(Const.Path.TEST_TASK_UPDATE);
        message.setBody(data);
        logger.info("Transfer json to byte: " + data);
        byte[] byteMsg = SerializeUtil.messageToByteArr(message);
        Assertions.assertNotNull(byteMsg, "Transfer to Byte error!");

        logger.info("Transfer byte to json: " + byteMsg);
        Message messageCopy = SerializeUtil.byteArrToMessage(byteMsg);
        Assertions.assertTrue(message.getPath().equals(messageCopy.getPath()), "Transfer to message error!");

        JSONObject dataCopy = (JSONObject) messageCopy.getBody();
        Assertions.assertTrue(data.getString(Const.AgentConfig.task_id_param).equals(dataCopy.getString(Const.AgentConfig.task_id_param)),
                "Transfer to message error!");
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
        logger.info(str1);
        Message msg = new Message();
        msg.setBody(array);
        Message msg2 = SerializeUtil.byteArrToMessage(SerializeUtil.messageToByteArr(msg));
        String str2 = ((JSONArray) msg2.getBody()).toJSONString();
        logger.info(str2);
        JSONArray array2 = JSONArray.parseArray(str2);
        String str3 = array2.toJSONString();
        logger.info(str3);
        Assertions.assertEquals(str1, str2, "Serialize error!");
        Assertions.assertEquals(str1, str3, "Serialize error!");
    }
}