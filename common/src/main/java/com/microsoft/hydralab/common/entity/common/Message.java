// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.util.Const;
import lombok.Data;
import org.apache.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

@Data
public class Message {
    String method = Message.HttpMethod.GET.toString();
    String sessionId;
    int code;
    String message;
    String agentId;
    String type;
    String path;
    Map<String, Object> params;
    Object body;
    String bodyType;

    public static Message ok(String path, Object body) {
        Message objectMessage = new Message();
        objectMessage.sessionId = UUID.randomUUID().toString();
        objectMessage.path = path;
        objectMessage.code = HttpStatus.SC_OK;
        objectMessage.setBody(body);
        return objectMessage;
    }

    public void setBody(Object body) {
        this.body = body;
        if (body != null && !(body instanceof JSONObject) && !(body instanceof JSONArray)) {
            bodyType = body.getClass().getName();
        }
    }

    public static Message error(Message source, int code, String msg) {
        Message objectMessage = new Message();
        objectMessage.sessionId = source.sessionId;
        objectMessage.path = source.path;
        objectMessage.code = code;
        objectMessage.message = msg;
        return objectMessage;
    }

    public static Message response(Message source, Object body) {
        Message objectMessage = new Message();
        objectMessage.sessionId = source.sessionId;
        objectMessage.path = source.path;
        objectMessage.setBody(body);
        return objectMessage;
    }

    public static Message auth() {
        Message objectMessage = new Message();
        objectMessage.sessionId = UUID.randomUUID().toString();
        objectMessage.setMethod(HttpMethod.GET.toString());
        objectMessage.setPath(Const.Path.AUTH);
        return objectMessage;
    }

    public enum HttpMethod {
        /**
         * The HTTP GET method.
         */
        GET,

        /**
         * The HTTP PUT method.
         */
        PUT,

        /**
         * The HTTP POST method.
         */
        POST,

        /**
         * The HTTP PATCH method.
         */
        PATCH,

        /**
         * The HTTP DELETE method.
         */
        DELETE,

        /**
         * The HTTP HEAD method.
         */
        HEAD,

        /**
         * The HTTP OPTIONS method.
         */
        OPTIONS,

        /**
         * The HTTP TRACE method.
         */
        TRACE,

        /**
         * The HTTP CONNECT method.
         */
        CONNECT
    }

}
