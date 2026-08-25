import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.common.entity.common.AgentUser;
import com.microsoft.hydralab.common.util.GlobalConstant;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class MetricUtilTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private DeviceAgentManagementService deviceAgentManagementService;

    private MetricUtil metricUtil;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        metricUtil = new MetricUtil();
        metricUtil.meterRegistry = meterRegistry;
    }

    @Test
    public void testRegisterAgentAliveStatusMetric() {
        AgentUser agentUser = new AgentUser();
        agentUser.setId("agentId");
        agentUser.setHostname("computerName");
        agentUser.setName("agentName");
        agentUser.setTeamName("teamName");

        metricUtil.registerAgentAliveStatusMetric(agentUser);

        verify(meterRegistry).gauge(eq(GlobalConstant.PROMETHEUS_METRIC_WEBSOCKET_DISCONNECT_SIGNAL),
                eq(Tags.empty().and("computerName", agentUser.getHostname(), "agentName", agentUser.getName(), "teamName", agentUser.getTeamName())),
                eq(agentUser.getId()),
                any());
    }
}