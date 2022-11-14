package com.microsoft.hydralab.common.util;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.common.Message;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;


public class SerializeUtilTest {


    @Test
    public void messageToByteArr() {
        JSONObject data = new JSONObject();
        Message message = new Message();
        data.put(Const.AgentConfig.task_id_param, UUID.randomUUID().toString());
        message.setPath(Const.Path.TEST_TASK_UPDATE);
        message.setBody(data);
        byte[] byteMsg = SerializeUtil.messageToByteArr(message);
        Assert.assertNotNull("Transfer to Byte error!", byteMsg);

        Message messageCopy = SerializeUtil.byteArrToMessage(byteMsg);
        Assert.assertTrue("Transfer to message error!", message.getPath().equals(messageCopy.getPath()));

        JSONObject dataCopy = (JSONObject) messageCopy.getBody();
        Assert.assertTrue("Transfer to message error!",
                data.getString(Const.AgentConfig.task_id_param).equals(dataCopy.getString(Const.AgentConfig.task_id_param)));
    }

}