package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.repository.AgentUserRepository;
import com.microsoft.hydralab.center.test.BaseTest;
import com.microsoft.hydralab.common.entity.common.AgentUser;
import com.microsoft.hydralab.common.entity.common.Message;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.SerializeUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;

import static org.mockito.BDDMockito.given;

public class DeviceAgentManagementServiceTest extends BaseTest {
    @Resource
    DeviceAgentManagementService deviceAgentManagementService;
    @MockBean
    AgentUserRepository agentUserRepository;
    @Resource
    ApplicationContext applicationContext;


    @Test
    public void testOnMessage() throws IOException {
        Session session = Mockito.mock(Session.class);
        RemoteEndpoint.Basic basicRemote = Mockito.mock(RemoteEndpoint.Basic.class);
        Mockito.when(session.getId()).thenReturn("123456");
        Mockito.when(session.getBasicRemote()).thenReturn(basicRemote);

        String agentId = "agentId";
        AgentUser agentUser = new AgentUser();
        agentUser.setId(agentId);
        agentUser.setName("agentName");
        agentUser.setSecret("agentSecret");
        agentUser.setTeamName("Microsoft");
        InetAddress localHost = InetAddress.getLocalHost();
        agentUser.setHostname(localHost.getHostName());
        agentUser.setIp("127.0.0.1");
        agentUser.setOs(System.getProperties().getProperty("os.name"));
        agentUser.setVersionName("versionName");
        agentUser.setVersionCode("versionCode");

        given(agentUserRepository.findById(agentId)).willReturn(Optional.of(agentUser));

        StorageServiceClientProxy storageServiceClientProxy = new StorageServiceClientProxy(applicationContext);
        storageServiceClientProxy.initCenterStorageClient("LOCAL");
        given(this.storageServiceClientProxy.generateAccessToken(Const.FilePermission.WRITE)).willReturn(storageServiceClientProxy.generateAccessToken(Const.FilePermission.WRITE));

        Message message = Message.ok(Const.Path.AUTH, agentUser);
        byte[] byteMsg = SerializeUtil.messageToByteArr(message);
        // will throw exception when invoking: com.microsoft.hydralab.center.service.StorageTokenManageService.generateWriteToken
        deviceAgentManagementService.onMessage(SerializeUtil.byteArrToMessage(byteMsg), session);
        session.close();
    }
}