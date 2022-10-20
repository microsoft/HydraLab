// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.center;

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
    private int agentDeviceType;
    private List<DeviceInfo> devices;

    public void initWithAgentUser(AgentUser agentUser) {
        agentId = agentUser.id;
        agentName = agentUser.name;
        agentOS = agentUser.os;
        agentRole = agentUser.role;
        agentDeviceType = agentUser.deviceType;
        teamId = agentUser.teamId;
        teamName = agentUser.teamName;
        userName = agentUser.mailAddress;
        hostname = agentUser.hostname;
        ip = agentUser.ip;
        agentVersionName = agentUser.versionName;
        agentVersionCode = agentUser.versionCode;
    }

    public interface Status {
        String HEALTHY = "HEALTHY";
        String ERROR = "ERROR";
        String UPDATING = "UPDATING";
    }
}
