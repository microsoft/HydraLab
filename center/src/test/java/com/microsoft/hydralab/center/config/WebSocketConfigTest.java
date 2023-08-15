package com.microsoft.hydralab.center.config;

import com.microsoft.hydralab.center.config.WebSocketConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertNotNull;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class WebSocketConfigTest {

    private WebSocketConfig webSocketConfig;
    @Mock
    private ServletServerContainerFactoryBean container;

    @Before
    public void setUp() {
        webSocketConfig = new WebSocketConfig();
    }

    @Test
    public void testServerEndpointExporter() {
        ServerEndpointExporter serverEndpointExporter = webSocketConfig.serverEndpointExporter();
        assertNotNull(serverEndpointExporter);
    }

    @Test
    public void testCreateWebSocketContainer() {
        when(container.getMaxTextMessageBufferSize()).thenReturn(100000);
        when(container.getMaxBinaryMessageBufferSize()).thenReturn(100000);
        ServletServerContainerFactoryBean result = webSocketConfig.createWebSocketContainer();
        assertSame(container, result);
    }
}