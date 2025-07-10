package com.microsoft.hydralab.agent.service;

import com.microsoft.hydralab.agent.test.BaseTest;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgentManageServiceTest extends BaseTest {

    @Resource
    AgentManageService agentManageService;

    @Test
    void getAgentAccessToken() {
        String accessToken = agentManageService.getAgentAccessToken("228c4d51-5002-4fcf-8752-552b478fff77");
        assertNotNull(accessToken, "Access token should not be null");
        // Additional assertions can be added based on expected token format or content
    }
}