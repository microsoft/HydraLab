import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.common.entity.common.AgentUser;
import com.microsoft.hydralab.common.util.GlobalConstant;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.HashMap;
import static org.mockito.Mockito.*;

public class MetricUtilTest {
@Mock
private DeviceAgentManagementService deviceAgentManagementService;
@Mock
private MeterRegistry meterRegistry;
private MetricUtil metricUtil;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); metricUtil = new MetricUtil(); metricUtil.meterRegistry = meterRegistry; 
}

@Test
public void testRegisterOnlineAgent() {
 String metricName = GlobalConstant.PROMETHEUS_METRIC_ONLINE_AGENT_NUM; Tags tags = Tags.empty(); metricUtil.registerOnlineAgent(deviceAgentManagementService); verify(meterRegistry).gauge(metricName, tags, deviceAgentManagementService, metricUtil::getOnlineAgentNum); verify(metricUtil).getOnlineAgentNum(deviceAgentManagementService); 
}

}
