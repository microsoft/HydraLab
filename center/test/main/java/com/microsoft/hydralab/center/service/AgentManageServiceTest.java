package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.repository.AgentUserRepository;
import com.microsoft.hydralab.center.service.AgentManageService;
import com.microsoft.hydralab.center.service.SysUserService;
import com.microsoft.hydralab.center.service.UserTeamManagementService;
import com.microsoft.hydralab.center.util.CenterConstant;
import com.microsoft.hydralab.center.util.SecretGenerator;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.AgentUser;
import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.util.CriteriaTypeUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AgentManageServiceTest {

    @Mock
    private AgentUserRepository agentUserRepository;

    @InjectMocks
    private AgentManageService agentManageService;

    @Mock
    private UserTeamManagementService userTeamManagementService;

    @Mock
    private SysUserService sysUserService;

    private SysUser sysUser;
    private AgentUser agentUser;
    private String teamId;
    private String teamName;
    private List<AgentUser> agents;

    @Before
    public void setup() {
        sysUser = new SysUser();
        sysUser.setMailAddress("test@mail.com");
        agentUser = new AgentUser();
        agentUser.setId("agentId");
        agentUser.setMailAddress("test@mail.com");
    }

    @Test
    public void testCreateAgent() {
        String teamId = "teamId";
        String teamName = "teamName";
        String mailAddress = "mailAddress";
        String os = "os";
        String name = "name";
        AgentUser agentUserInfo = new AgentUser();
        agentUserInfo.setMailAddress(mailAddress);
        agentUserInfo.setOs(os);
        agentUserInfo.setName(name);
        agentUserInfo.setTeamId(teamId);
        agentUserInfo.setTeamName(teamName);
        SecretGenerator secretGenerator = new SecretGenerator();
        String agentSecret = secretGenerator.generateSecret();
        agentUserInfo.setSecret(agentSecret);
        when(agentUserRepository.saveAndFlush(any(AgentUser.class))).thenReturn(agentUserInfo);
        AgentUser result = agentManageService.createAgent(teamId, teamName, mailAddress, os, name);
        assertNotNull(result);
        assertEquals(mailAddress, result.getMailAddress());
        assertEquals(os, result.getOs());
        assertEquals(name, result.getName());
        assertEquals(teamId, result.getTeamId());
        assertEquals(teamName, result.getTeamName());
        assertEquals(agentSecret, result.getSecret());
        verify(agentUserRepository, times(1)).saveAndFlush(any(AgentUser.class));
    }

    @Before
    public void setUp() {
        teamId = "teamId";
        teamName = "teamName";
        agents = new ArrayList<>();
        agents.add(new AgentUser());
        agents.add(new AgentUser());
    }

    @Test
    public void testGetAgent() {
        String agentId = "agent123";
        AgentUser agentUser = new AgentUser();
        agentUser.setId(agentId);
        Optional<AgentUser> optionalAgentUser = Optional.of(agentUser);
        when(agentUserRepository.findById(agentId)).thenReturn(optionalAgentUser);
        AgentUser result = agentManageService.getAgent(agentId);
        assertEquals(agentUser, result);
    }

    @Test
    public void testDeleteAgent() {
        AgentUser agentUser = new AgentUser();
        agentManageService.deleteAgent(agentUser);
        verify(agentUserRepository, times(1)).delete(agentUser);
    }

    @Test
    public void testIsAgentNameRegistered() {
        String agentName = "agent1";
        AgentUser agentUser = new AgentUser();
        agentUser.setName(agentName);
        Mockito.when(agentUserRepository.findByName(agentName)).thenReturn(Optional.of(agentUser));
        boolean result = agentManageService.isAgentNameRegistered(agentName);
        Assert.assertTrue(result);
    }

    @Test
    public void testCheckAgentAuthorization_WithNullRequestor_ReturnsFalse() {
        assertFalse(agentManageService.checkAgentAuthorization(null, "agentId"));
    }

    @Test
    public void testCheckAgentAuthorization_WithNonExistingAgent_ReturnsFalse() {
        Mockito.when(agentUserRepository.findById("agentId")).thenReturn(null);
        assertFalse(agentManageService.checkAgentAuthorization(sysUser, "agentId"));
    }

    @Test
    public void testCheckAgentAuthorization_WithMatchingMailAddress_ReturnsTrue() {
        Mockito.when(agentUserRepository.findById("agentId")).thenReturn(agentUser);
        assertTrue(agentManageService.checkAgentAuthorization(sysUser, "agentId"));
    }

    @Test
    public void testCheckAgentAuthorization_WithAdminUser_ReturnsTrue() {
        Mockito.when(agentUserRepository.findById("agentId")).thenReturn(agentUser);
        Mockito.when(sysUserService.checkUserAdmin(sysUser)).thenReturn(true);
        assertTrue(agentManageService.checkAgentAuthorization(sysUser, "agentId"));
    }

    @Test
    public void testCheckAgentAuthorization_WithTeamAdmin_ReturnsTrue() {
        Mockito.when(agentUserRepository.findById("agentId")).thenReturn(agentUser);
        Mockito.when(userTeamManagementService.checkRequestorTeamAdmin(sysUser, "teamId")).thenReturn(true);
        assertTrue(agentManageService.checkAgentAuthorization(sysUser, "agentId"));
    }

    @Test
    public void testUpdateAgentTeam() {
        when(agentUserRepository.findAllByTeamId(teamId)).thenReturn(agents);
        agentManageService.updateAgentTeam(teamId, teamName);
        verify(agentUserRepository).saveAll(agents);
        for (AgentUser agent : agents) {
            assertEquals(teamName, agent.getTeamName());
        }
    }

    @Test
    public void testGenerateAgentConfigFile() {
        String agentId = "agentId";
        String host = "localhost";
        AgentUser agentUser = new AgentUser();
        agentUser.setId(agentId);
        agentUser.setName("Agent Name");
        agentUser.setSecret("Agent Secret");
        when(agentUserRepository.findById(agentId)).thenReturn(Optional.of(agentUser));
        File agentConfigFile = agentManageService.generateAgentConfigFile(agentId, host);
        assertNotNull(agentConfigFile);
        assertTrue(agentConfigFile.exists());
        assertEquals("application.yml", agentConfigFile.getName());
    }

}
