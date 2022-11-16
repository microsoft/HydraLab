package com.microsoft.hydralab.common.util;

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

}