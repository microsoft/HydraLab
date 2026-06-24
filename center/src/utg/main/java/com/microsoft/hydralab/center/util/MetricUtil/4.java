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
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

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
public void testGetAgentAliveStatus() {
 String agentUserId = "agent1"; String agentStatus = GlobalConstant.AgentLiveStatus.ONLINE.getStatus(); HashMap<String, String> agentAliveStatusMap = new HashMap<>(); agentAliveStatusMap.put(agentUserId, agentStatus); metricUtil.agentAliveStatusMap = agentAliveStatusMap; int expected = 0; if (GlobalConstant.AgentLiveStatus.OFFLINE.getStatus().equals(agentStatus)) { expected = 1; } int actual = metricUtil.getAgentAliveStatus(agentUserId); assertEquals(expected, actual); 
}

}
