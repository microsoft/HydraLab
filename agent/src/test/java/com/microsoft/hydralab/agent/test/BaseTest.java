// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.test;

import com.microsoft.hydralab.agent.service.AgentWebSocketClientService;
import com.microsoft.hydralab.agent.socket.AgentWebSocketClient;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.transaction.Transactional;

/**
 * @author zhoule
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@Transactional
@Rollback
@Disabled
public class BaseTest {
    protected Logger baseLogger = LoggerFactory.getLogger(BaseTest.class);
    @MockBean
    StorageServiceClientProxy storageServiceClientProxy;
    @MockBean
    AgentWebSocketClient agentWebSocketClient;
    @MockBean
    AgentWebSocketClientService agentWebSocketClientService;
}
