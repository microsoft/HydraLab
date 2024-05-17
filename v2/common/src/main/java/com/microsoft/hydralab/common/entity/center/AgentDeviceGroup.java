// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.center;

import com.microsoft.hydralab.common.entity.agent.AgentFunctionAvailability;
import com.microsoft.hydralab.common.entity.common.AgentUser;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import lombok.Data;

import jakarta.persistence.Transient;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Data
public class AgentDeviceGroup {
    @Transient
    private static final int DEFAULT_ANALYSIS_COUNT = 3;

    private String agentId;
    private String agentName;
    private String agentOS;
    private String agentRole;
    private String agentStatus = Status.HEALTHY;
    private String agentMessage;
    private String agentVersionName;
    private String agentVersionCode;
    private String teamId;
    private String teamName;
    private String userName;
    private String hostname;
    private String ip;
    private List<DeviceInfo> devices;
    private List<AgentFunctionAvailability> functionAvailabilities;
    private ConcurrentMap<String, Integer> availableAnalysisTaskCount = new ConcurrentHashMap<>();

    public void initWithAgentUser(AgentUser agentUser) {
        agentId = agentUser.getId();
        agentName = agentUser.getName();
        agentOS = agentUser.getOs();
        agentRole = agentUser.getRole();
        teamId = agentUser.getTeamId();
        teamName = agentUser.getTeamName();
        userName = agentUser.getMailAddress();
        hostname = agentUser.getHostname();
        ip = agentUser.getIp();
        agentVersionName = agentUser.getVersionName();
        agentVersionCode = agentUser.getVersionCode();
        functionAvailabilities = agentUser.getFunctionAvailabilities();
        for (AgentFunctionAvailability functionAvailability : functionAvailabilities) {
            if (AgentFunctionAvailability.AgentFunctionType.ANALYSIS_RUNNER.equals(functionAvailability.getFunctionType()) && functionAvailability.isEnabled()) {
                availableAnalysisTaskCount.put(functionAvailability.getFunctionName(), 3);
            }
        }
    }

    public interface Status {
        String HEALTHY = "HEALTHY";
        String ERROR = "ERROR";
        String UPDATING = "UPDATING";
    }

    public void runAnalysisTask(String runnerType) {
        availableAnalysisTaskCount.put(runnerType, availableAnalysisTaskCount.getOrDefault(runnerType, DEFAULT_ANALYSIS_COUNT) - 1);
    }

    public void finishAnalysisTask(String runnerType) {
        if (availableAnalysisTaskCount.get(runnerType) == null) {
            return;
        }
        availableAnalysisTaskCount.put(runnerType, availableAnalysisTaskCount.get(runnerType) + 1);
    }
}
