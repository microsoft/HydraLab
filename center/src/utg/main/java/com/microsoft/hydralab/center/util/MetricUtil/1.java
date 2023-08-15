import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

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
public void testRegisterOnlineDevice() {
 metricUtil.registerOnlineDevice(deviceAgentManagementService); verify(meterRegistry).gauge(eq(GlobalConstant.PROMETHEUS_METRIC_ONLINE_DEVICE_NUM), eq(Tags.empty()), eq(deviceAgentManagementService), any()); verify(deviceAgentManagementService).getAliveDeviceNum(); verifyNoMoreInteractions(meterRegistry, deviceAgentManagementService); 
}

}
