import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.HashMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class MetricUtilTest {
@Mock
private DeviceAgentManagementService deviceAgentManagementService;
@Mock
private MeterRegistry meterRegistry;
private MetricUtil metricUtil;
@Before
public void setUp() {
 MockitoAnnotations.initMocks(this); metricUtil = new MetricUtil(); metricUtil.meterRegistry = meterRegistry; 
}

@Test
public void testGetAliveDeviceNum() {
 int expected = 5; when(deviceAgentManagementService.getAliveDeviceNum()).thenReturn(expected); int actual = metricUtil.getAliveDeviceNum(deviceAgentManagementService); assertEquals(expected, actual); 
}

}
