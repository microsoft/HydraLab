package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.repository.AgentUserRepository;
import com.microsoft.hydralab.center.test.BaseTest;
import com.microsoft.hydralab.common.entity.common.AgentUser;
import com.microsoft.hydralab.common.entity.common.BlockedDeviceInfo;
import com.microsoft.hydralab.common.entity.common.Message;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.file.impl.local.LocalStorageClientAdapter;
import com.microsoft.hydralab.common.file.impl.local.LocalStorageProperty;
import com.microsoft.hydralab.common.repository.BlockedDeviceInfoRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.BDDMockito.given;

public class DeviceAgentManagementServiceTest extends BaseTest {
    @Resource
    LocalStorageProperty localStorageProperty;
    @Resource
    AgentUserRepository agentUserRepository;
    @Resource
    DeviceAgentManagementService deviceAgentManagementService;
    @Resource
    BlockedDeviceInfoRepository blockedDeviceInfoRepository;

    private final ConcurrentHashMap<String, BlockedDeviceInfo> blockedDevicesMap = new ConcurrentHashMap<>();

    @Test
    public void testOnMessage_NoException() throws IOException {
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

        agentUserRepository.saveAndFlush(agentUser);

        LocalStorageClientAdapter localStorageClientAdapter = new LocalStorageClientAdapter(localStorageProperty);
        given(this.storageServiceClientProxy.generateAccessToken(Const.FilePermission.WRITE)).willReturn(localStorageClientAdapter.generateAccessToken(Const.FilePermission.WRITE));

        Message message = Message.ok(Const.Path.AUTH, agentUser);
        byte[] byteMsg = SerializeUtil.messageToByteArr(message);

        deviceAgentManagementService.onMessage(SerializeUtil.byteArrToMessage(byteMsg), session);
        session.close();
    }

    @Test
    public void blockDevice() {
        BlockedDeviceInfo blockedDeviceInfo = new BlockedDeviceInfo();
        blockedDeviceInfo.setBlockedDeviceSerialNumber("123456");
        blockedDeviceInfo.setBlockedTime(Instant.now());
        blockedDeviceInfo.setBlockingTaskUUID("88888");

        synchronized (blockedDevicesMap) {
            blockedDevicesMap.put(blockedDeviceInfo.getBlockedDeviceSerialNumber(), blockedDeviceInfo);
            blockedDeviceInfoRepository.save(blockedDeviceInfo);

            if (blockedDeviceInfoRepository.existsByBlockedDeviceSerialNumber(blockedDeviceInfo.getBlockedDeviceSerialNumber())) {
                blockedDeviceInfoRepository.deleteByBlockedDeviceSerialNumber(blockedDeviceInfo.getBlockedDeviceSerialNumber());
            }

            List<BlockedDeviceInfo> blockedDeviceInfoList = blockedDeviceInfoRepository.findAll();
            for (BlockedDeviceInfo blockedDeviceInfo1 : blockedDeviceInfoList) {
                blockedDeviceInfoRepository.deleteByBlockedTimeBefore(Instant.now());
            }

        }


    }
}
