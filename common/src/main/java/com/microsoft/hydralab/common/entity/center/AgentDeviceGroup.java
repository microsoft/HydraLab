// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.center;

import com.microsoft.hydralab.common.entity.common.AgentUser;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import lombok.Data;

import java.util.List;

@Data
public class AgentDeviceGroup {
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
    }

    public interface Status {
        String HEALTHY = "HEALTHY";
        String ERROR = "ERROR";
        String UPDATING = "UPDATING";
    }
}
