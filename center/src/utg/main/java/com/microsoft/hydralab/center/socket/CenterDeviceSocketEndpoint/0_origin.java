import com.microsoft.hydralab.center.config.SpringApplicationListener;
import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.websocket.Session;

import static org.mockito.Mockito.verify;

public class CenterDeviceSocketEndpointTest {

    @Mock
    private DeviceAgentManagementService deviceAgentManagementService;

    @Mock
    private Session session;

    private CenterDeviceSocketEndpoint centerDeviceSocketEndpoint;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        centerDeviceSocketEndpoint = new CenterDeviceSocketEndpoint();
        SpringApplicationListener.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(centerDeviceSocketEndpoint);
    }

    @Test
    public void testOnOpen() {
        centerDeviceSocketEndpoint.onOpen(session);
        verify(deviceAgentManagementService).onOpen(session);
    }
}