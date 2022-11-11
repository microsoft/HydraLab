// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.test;

import com.microsoft.hydralab.agent.runner.TestRunningCallback;
import com.microsoft.hydralab.agent.service.AgentWebSocketClientService;
import com.microsoft.hydralab.agent.socket.AgentWebSocketClient;
import com.microsoft.hydralab.agent.util.MetricUtil;
import com.microsoft.hydralab.common.util.blob.BlobStorageClient;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;

/**
 * @author zhoule
 * @date 11/10/2022
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
@Rollback
public class BaseTest {
    protected Logger baseLogger = LoggerFactory.getLogger(BaseTest.class);
    @MockBean
    BlobStorageClient blobStorageClient;
    @MockBean
    AgentWebSocketClient AgentWebSocketClient;
    @MockBean
    AgentWebSocketClientService agentWebSocketClientService;
    @MockBean
    @Qualifier("getMetricUtil")
    MetricUtil metricUtil;
}
