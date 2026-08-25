import com.microsoft.hydralab.center.config.ContextStartup;
import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.center.util.MetricUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.ServletContext;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"default", "release", "dev"})
public class ContextStartupTest {

    @Mock
    private MetricUtil metricUtil;

    @Mock
    private DeviceAgentManagementService deviceAgentManagementService;

    @Mock
    private ApplicationArguments applicationArguments;

    @Mock
    private ServletContext servletContext;

    private ContextStartup contextStartup;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        contextStartup = new ContextStartup();
        contextStartup.setServletContext(servletContext);
    }

    @Test
    public void testRun() {
        contextStartup.run(applicationArguments);
        verify(deviceAgentManagementService, times(1)).registerOnlineAgent(deviceAgentManagementService);
        verify(deviceAgentManagementService, times(1)).registerOnlineDevice(deviceAgentManagementService);
    }
}