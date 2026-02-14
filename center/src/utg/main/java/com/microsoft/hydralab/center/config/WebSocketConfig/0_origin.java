import com.microsoft.hydralab.center.config.WebSocketConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;

public class WebSocketConfigTest {
    private WebSocketConfig webSocketConfig;

    @Before
    public void setUp() {
        webSocketConfig = new WebSocketConfig();
    }

    @Test
    public void testServerEndpointExporter() {
        ServerEndpointExporter serverEndpointExporter = webSocketConfig.serverEndpointExporter();
        assertNotNull(serverEndpointExporter);
    }
}