import static org.mockito.Mockito.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.common.entity.common.Message;
import com.microsoft.hydralab.common.util.SerializeUtil;

public class CenterDeviceSocketEndpointTest {
@Mock
private DeviceAgentManagementService deviceAgentManagementService;
@Mock
private Session session;
private CenterDeviceSocketEndpoint centerDeviceSocketEndpoint;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); centerDeviceSocketEndpoint = new CenterDeviceSocketEndpoint(); centerDeviceSocketEndpoint.deviceAgentManagementService = deviceAgentManagementService; 
}

@Test
public void testOnMessage() {
 ByteBuffer message = ByteBuffer.allocate(10); Message formattedMessage = new Message(); when(SerializeUtil.byteArrToMessage(message.array())).thenReturn(formattedMessage); centerDeviceSocketEndpoint.onMessage(message, session); verify(deviceAgentManagementService).onMessage(formattedMessage, session); 
}

}
