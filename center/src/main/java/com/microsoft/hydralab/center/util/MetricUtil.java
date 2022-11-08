// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.util;

import com.microsoft.hydralab.common.entity.center.AgentUser;
import com.microsoft.hydralab.common.util.GlobalConstant;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;

@Service
@Slf4j
public class MetricUtil {
    @Resource
    MeterRegistry meterRegistry;
    //save agent status <AgentUser ID, live status>
    private final HashMap<String, String> agentAliveStatusMap = new HashMap<>();

    public void registerAgentAliveStatusMetric(AgentUser agentUser) {
        if (agentAliveStatusMap.containsKey(agentUser.getId())) {
            updateAgentAliveStatus(agentUser.getId(), GlobalConstant.AgentLiveStatus.ONLINE.getStatus());
            return;
        }
        updateAgentAliveStatus(agentUser.getId(), GlobalConstant.AgentLiveStatus.ONLINE.getStatus());

        meterRegistry.gauge(GlobalConstant.PROMETHEUS_METRIC_WEBSOCKET_DISCONNECT_SIGNAL,
                Tags.empty().and("computerName", agentUser.getHostname(), "agentName", agentUser.getName(), "teamName", agentUser.getTeamName()),
                agentUser.getId(),
                this::getAgentAliveStatus);
        log.info("Status metric of agent {} has been registered.", agentUser.getName());
    }

    public void updateAgentAliveStatus(String agentId, String status) {
        agentAliveStatusMap.put(agentId, status);
    }


    public int getAgentAliveStatus(String agentUserId) {
        String agentStatus = agentAliveStatusMap.getOrDefault(agentUserId, GlobalConstant.AgentLiveStatus.OFFLINE.getStatus());
        return GlobalConstant.AgentLiveStatus.OFFLINE.getStatus().equals(agentStatus) ? 1 : 0;
    }


}
