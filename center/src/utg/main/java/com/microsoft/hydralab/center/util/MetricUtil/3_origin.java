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
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        metricUtil = new MetricUtil();
        metricUtil.meterRegistry = meterRegistry;
    }

    @Test
    public void testUpdateAgentAliveStatus() {
        String agentId = "agent1";
        String status = "online";

        metricUtil.updateAgentAliveStatus(agentId, status);

        verify(meterRegistry, times(0)).gauge(anyString(), any(Tags.class), any(), any());
        verifyNoMoreInteractions(meterRegistry);
    }
}