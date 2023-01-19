package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.common.entity.center.AgentUser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class AgentManageServiceTest {

    @Test
    void putData() {
        String agentId = "test_agent_id";
        AgentManageService agentManageService = Mockito.spy(AgentManageService.class);
        AgentUser agent = new AgentUser();
        agent.setName("Agent Name");
        agent.setId(agentId);
        agent.setSecret("Agent Secret");
        doReturn(agent).when(agentManageService).getAgent(agentId);

        agentManageService.generateAgentConfigFile(agentId);
    }
}
