import com.microsoft.hydralab.center.socket.CenterDeviceSocketEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import javax.websocket.Session;
import static org.mockito.Mockito.verify;

public class CenterDeviceSocketEndpointTest {
@Mock
private Session session;
@Mock
private Throwable error;
private CenterDeviceSocketEndpoint centerDeviceSocketEndpoint;
@Before
public void setUp() {
 MockitoAnnotations.initMocks(this); centerDeviceSocketEndpoint = new CenterDeviceSocketEndpoint(); 
}

@Test
public void testOnError() {
 centerDeviceSocketEndpoint.onError(session, error); verify(centerDeviceSocketEndpoint.getDeviceAgentManagementService()).onError(session, error); 
}

}
