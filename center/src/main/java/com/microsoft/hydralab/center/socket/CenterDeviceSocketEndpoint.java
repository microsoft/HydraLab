// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.socket;

import com.microsoft.hydralab.center.config.SpringApplicationListener;
import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.common.util.SerializeUtil;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
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
        deviceAgentManagementService.onMessage(SerializeUtil.byteArrToMessage(message.array()), session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        deviceAgentManagementService.onError(session, error);
    }

}


