package com.microsoft.hydralab.center.socket;

import com.microsoft.hydralab.center.config.SpringApplicationListener;
import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.center.socket.CenterDeviceSocketEndpoint;
import com.microsoft.hydralab.common.entity.common.Message;
import com.microsoft.hydralab.common.util.SerializeUtil;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.websocket.Session;
import javax.websocket.CloseReason;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CenterDeviceSocketEndpointTest {

    @Mock
    private DeviceAgentManagementService deviceAgentManagementService;
    @Mock
    private Session session;
    private CenterDeviceSocketEndpoint centerDeviceSocketEndpoint;
    @Mock
    private Throwable error;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        centerDeviceSocketEndpoint = new CenterDeviceSocketEndpoint();
    }

    @Test
    public void testOnOpen() {
        centerDeviceSocketEndpoint.onOpen(session);
        verify(deviceAgentManagementService).onOpen(session);
    }

    @Test
    public void testOnClose() {
        centerDeviceSocketEndpoint.onClose(session);
    }

    @Test
    public void testOnMessage() {
        ByteBuffer message = ByteBuffer.allocate(10);
        Message formattedMessage = new Message();
        when(SerializeUtil.byteArrToMessage(message.array())).thenReturn(formattedMessage);
        centerDeviceSocketEndpoint.onMessage(message, session);
        verify(deviceAgentManagementService).onMessage(formattedMessage, session);
    }

    @Test
    public void testOnError() {
        centerDeviceSocketEndpoint.onError(session, error);
        verify(centerDeviceSocketEndpoint.getDeviceAgentManagementService()).onError(session, error);
    }
}