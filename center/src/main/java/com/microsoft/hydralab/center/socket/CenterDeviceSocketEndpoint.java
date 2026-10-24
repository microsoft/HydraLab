// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.socket;

import com.microsoft.hydralab.center.config.SpringApplicationListener;
import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.common.entity.common.Message;
import com.microsoft.hydralab.common.util.SerializeUtil;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Notice: this is not a Spring governed instance and its instance is created and managed by JWA implementation.
 */
@ServerEndpoint(value = "/agent/connect")
@Component
@DependsOn("SpringApplicationListener")
public class CenterDeviceSocketEndpoint {

    @Resource
    DeviceAgentManagementService deviceAgentManagementService;

    public CenterDeviceSocketEndpoint() {
        SpringApplicationListener.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(this);
    }

    @OnOpen
    public void onOpen(Session session) {
        deviceAgentManagementService.onOpen(session);
    }

    @OnClose
    public void onClose(Session session) {
        deviceAgentManagementService.onClose(session);
    }

    @OnMessage
    public void onMessage(ByteBuffer message, Session session) {
        Message formattedMessage;
        try {
            formattedMessage = SerializeUtil.byteArrToMessage(message.array());
        } catch (Exception e) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Message format error, please update your agent."));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }
        try {
            deviceAgentManagementService.onMessage(formattedMessage, session);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        deviceAgentManagementService.onError(session, error);
    }

}


