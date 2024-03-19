package com.microsoft.hydralab.center.util;

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
        MockitoAnnotations.initMocks(this);
        metricUtil = new MetricUtil();
        metricUtil.meterRegistry = meterRegistry;
    }

    @Test
    public void testRegisterOnlineAgent() {
        String metricName = GlobalConstant.PROMETHEUS_METRIC_ONLINE_AGENT_NUM;
        Tags tags = Tags.empty();
        metricUtil.registerOnlineAgent(deviceAgentManagementService);
        verify(meterRegistry).gauge(metricName, tags, deviceAgentManagementService, metricUtil::getOnlineAgentNum);
        verify(metricUtil).getOnlineAgentNum(deviceAgentManagementService);
    }

    @Test
    public void testRegisterOnlineDevice() {
        metricUtil.registerOnlineDevice(deviceAgentManagementService);
        verify(meterRegistry).gauge(eq(GlobalConstant.PROMETHEUS_METRIC_ONLINE_DEVICE_NUM), eq(Tags.empty()), eq(deviceAgentManagementService), any());
        verify(deviceAgentManagementService).getAliveDeviceNum();
        verifyNoMoreInteractions(meterRegistry, deviceAgentManagementService);
    }

    @Test
    public void testRegisterAgentAliveStatusMetric() {
        AgentUser agentUser = new AgentUser();
        agentUser.setId("agentId");
        agentUser.setHostname("computerName");
        agentUser.setName("agentName");
        agentUser.setTeamName("teamName");
        metricUtil.registerAgentAliveStatusMetric(agentUser);
        verify(meterRegistry).gauge(eq(GlobalConstant.PROMETHEUS_METRIC_WEBSOCKET_DISCONNECT_SIGNAL), eq(Tags.empty().and("computerName", agentUser.getHostname(), "agentName", agentUser.getName(), "teamName", agentUser.getTeamName())), eq(agentUser.getId()), any());
    }

    @Test
    public void testUpdateAgentAliveStatus() {
        String agentId = "agent1";
        String status = "online";
        metricUtil.updateAgentAliveStatus(agentId, status);
        verify(meterRegistry, times(0)).gauge(anyString(), any(Tags.class), any(), any());
        verifyNoMoreInteractions(meterRegistry);
    }

    @Test
    public void testGetAgentAliveStatus() {
        String agentUserId = "agent1";
        String agentStatus = GlobalConstant.AgentLiveStatus.ONLINE.getStatus();
        HashMap<String, String> agentAliveStatusMap = new HashMap<>();
        agentAliveStatusMap.put(agentUserId, agentStatus);
        metricUtil.agentAliveStatusMap = agentAliveStatusMap;
        int expected = 0;
        if (GlobalConstant.AgentLiveStatus.OFFLINE.getStatus().equals(agentStatus)) {
            expected = 1;
        }
        int actual = metricUtil.getAgentAliveStatus(agentUserId);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetOnlineAgentNum() {
        int expected = 5;
        when(deviceAgentManagementService.getAliveAgentNum()).thenReturn(expected);
        int actual = metricUtil.getOnlineAgentNum(deviceAgentManagementService);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetAliveDeviceNum() {
        int expected = 5;
        when(deviceAgentManagementService.getAliveDeviceNum()).thenReturn(expected);
        int actual = metricUtil.getAliveDeviceNum(deviceAgentManagementService);
        assertEquals(expected, actual);
    }

}
