import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.websocket.Session;

public class CenterDeviceSocketEndpointTest {

    @Mock
    private DeviceAgentManagementService deviceAgentManagementService;

    private CenterDeviceSocketEndpoint centerDeviceSocketEndpoint;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        centerDeviceSocketEndpoint = new CenterDeviceSocketEndpoint();
        centerDeviceSocketEndpoint.deviceAgentManagementService = deviceAgentManagementService;
    }

    @Test
    public void testOnClose() {
        Session session = new Session();
        centerDeviceSocketEndpoint.onClose(session);
        // Add assertions here if needed
    }
}