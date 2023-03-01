package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.common.entity.common.AgentUser;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.mockito.Mockito.doReturn;

public class AgentManageServiceTest {

    @Test
    void generateAgentConfig() throws IOException {
        String agentId = "test_agent_id";
        String host = "localhost:9886";

        AgentManageService agentManageService = Mockito.spy(AgentManageService.class);
        AgentUser agent = new AgentUser();
        agent.setName("Agent Name");
        agent.setId(agentId);
        agent.setSecret("Agent Secret");
        doReturn(agent).when(agentManageService).getAgent(agentId);

        File file = agentManageService.generateAgentConfigFile(agentId, host);
        Assertions.assertNotNull(file, "Get agent user error");
        Assertions.assertTrue(file.length() > 0, "Write agent config file error");

        Assertions.assertTrue(IOUtils.toString(new FileReader(file)).contains(host));
    }
}
