// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import cn.hutool.core.util.ZipUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.common.Message;

import java.nio.charset.StandardCharsets;

public class SerializeUtil {
    public static byte[] messageToByteArr(Message message) {
        return ZipUtil.gzip(JSON.toJSONString(message), StandardCharsets.UTF_8.toString());
    }

    public static Message byteArrToMessage(byte[] array) {
        Message message = JSON.parseObject(new String(ZipUtil.unGzip(array), StandardCharsets.UTF_8), Message.class);
        if (message.getBody() instanceof JSONObject && message.getBodyType() != null) {
            JSONObject body = (JSONObject) message.getBody();
            try {
                message.setBody(body.toJavaObject(Class.forName(message.getBodyType())));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return message;
    }
}
