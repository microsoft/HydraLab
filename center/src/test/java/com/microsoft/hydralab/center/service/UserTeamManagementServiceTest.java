package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.repository.UserTeamRelationRepository;
import com.microsoft.hydralab.common.entity.center.SysRole;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.center.UserTeamRelation;
import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.util.Const;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.CollectionUtils;

import java.util.*;

import com.microsoft.hydralab.center.service.SysRoleService;
import com.microsoft.hydralab.center.service.SysTeamService;
import com.microsoft.hydralab.center.service.SysUserService;
import com.microsoft.hydralab.center.service.UserTeamManagementService;

import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.anyString;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)

public class UserTeamManagementServiceTest {

    @Mock
    private UserTeamRelationRepository userTeamRelationRepository;
    @Mock
    private SysTeamService sysTeamService;
    @Mock
    private SysUserService sysUserService;
    @Mock
    private SysRoleService sysRoleService;
    @InjectMocks
    private UserTeamManagementService userTeamManagementService;
    private SysUser requestor;
    private String teamId;
    private SysTeam team;

    @Before
    public void setup() {
        requestor = new SysUser();
        teamId = "team1";
    }

    @Test
    public void testInitList() {
        List<UserTeamRelation> relationList = new ArrayList<>();
        UserTeamRelation relation = new UserTeamRelation("teamId", "mailAddress", true);
        relationList.add(relation);
        Mockito.when(userTeamRelationRepository.findAll()).thenReturn(relationList);
        SysTeam team = new SysTeam();
        Mockito.when(sysTeamService.queryTeamById(Mockito.anyString())).thenReturn(team);
        SysUser user = new SysUser();
        Mockito.when(sysUserService.queryUserByMailAddress(Mockito.anyString())).thenReturn(user);
        userTeamManagementService.initList();
    }

    @Before
    public void setUp() {
        team = new SysTeam();
        team.setTeamId("teamId");
    }

    @Test
    public void testAddUserTeamRelation() {
        String teamId = "team1";
        SysUser user = new SysUser();
        user.setMailAddress("user1@example.com");
        boolean isTeamAdmin = true;
        SysTeam team = new SysTeam();
        team.setTeamId(teamId);
        when(sysTeamService.queryTeamById(teamId)).thenReturn(team);
        UserTeamRelation result = userTeamManagementService.addUserTeamRelation(teamId, user, isTeamAdmin);
        assertEquals(teamId, result.getTeamId());
        assertEquals(user.getMailAddress(), result.getMailAddress());
        assertEquals(isTeamAdmin, result.isTeamAdmin());
        verify(userTeamRelationRepository, times(1)).save(result);
    }

    @Test
    public void testDeleteUserTeamRelation() {
        UserTeamRelation relation = new UserTeamRelation("teamId", "mailAddress", true);
        userTeamManagementService.deleteUserTeamRelation(relation);
        verify(userTeamRelationRepository, times(1)).delete(relation);
    }

    @Test
    public void testQueryRelation() {
        UserTeamRelation result = userTeamManagementService.queryRelation("mailAddress", "teamId");
        assertEquals("teamId", result.getTeamId());
        assertEquals("mailAddress", result.getMailAddress());
        assertEquals(true, result.isTeamAdmin());
    }

    @Test
    public void testCheckUserExistenceWithTeam_Exist() {
        List<SysUser> users = new ArrayList<>();
        SysUser user1 = new SysUser();
        user1.setMailAddress("user1@example.com");
        users.add(user1);
        Mockito.when(userTeamManagementService.queryUsersByTeam(teamId)).thenReturn(users);
        boolean result = userTeamManagementService.checkUserExistenceWithTeam(requestor, teamId);
        assertTrue(result);
    }

    @Test
    public void testCheckUserExistenceWithTeam_NotExist() {
        List<SysUser> users = new ArrayList<>();
        Mockito.when(userTeamManagementService.queryUsersByTeam(teamId)).thenReturn(users);
        boolean result = userTeamManagementService.checkUserExistenceWithTeam(requestor, teamId);
        assertFalse(result);
    }

    @Test
    public void testCheckTeamAdmin() {
        String teamId = "123";
        String mailAddress = "test@example.com";
        Mockito.when(userTeamManagementService.checkTeamAdmin(teamId, mailAddress)).thenReturn(true);
        boolean result = userTeamManagementService.checkTeamAdmin(teamId, mailAddress);
        assertTrue(result);
    }

    @Test
    public void testCheckUserTeamRelation() {
        String mailAddress = "test@example.com";
        String teamId = "12345";
        UserTeamRelation userTeamRelation = new UserTeamRelation(teamId, mailAddress, false);
        Mockito.when(userTeamRelationRepository.findByMailAddressAndTeamId(mailAddress, teamId)).thenReturn(Optional.of(userTeamRelation));
        boolean result = userTeamManagementService.checkUserTeamRelation(mailAddress, teamId);
        assertTrue(result);
    }

    @Test
    public void testCheckRequestorTeamRelation_WhenRequestorIsTeamAdmin_ReturnsTrue() {
        Set<String> teamAdmins = new HashSet<>();
        teamAdmins.add(requestor.getMailAddress());
        Mockito.when(userTeamManagementService.checkRequestorTeamAdmin(requestor, teamId)).thenReturn(true);
        Mockito.when(userTeamManagementService.checkTeamAdmin(teamId, requestor.getMailAddress())).thenReturn(true);
        boolean result = userTeamManagementService.checkRequestorTeamRelation(requestor, teamId);
        assertTrue(result);
    }

    @Test
    public void testCheckRequestorTeamRelation_WhenRequestorIsNotTeamAdmin_ReturnsFalse() {
        Set<String> teamAdmins = new HashSet<>();
        teamAdmins.add(requestor.getMailAddress());
        Mockito.when(userTeamManagementService.checkRequestorTeamAdmin(requestor, teamId)).thenReturn(false);
        Mockito.when(userTeamManagementService.checkTeamAdmin(teamId, requestor.getMailAddress())).thenReturn(false);
        boolean result = userTeamManagementService.checkRequestorTeamRelation(requestor, teamId);
        assertFalse(result);
    }

    @Test
    public void testCheckRequestorTeamAdmin_WhenRequestorIsTeamAdmin_ReturnsTrue() {
        requestor.setTeamAdminMap(Map.of(teamId, true));
        boolean result = userTeamManagementService.checkRequestorTeamAdmin(requestor, teamId);
        assertTrue(result);
    }

    @Test
    public void testCheckRequestorTeamAdmin_WhenRequestorIsNotTeamAdmin_ReturnsFalse() {
        requestor.setTeamAdminMap(Map.of(teamId, false));
        boolean result = userTeamManagementService.checkRequestorTeamAdmin(requestor, teamId);
        assertFalse(result);
    }

    @Test
    public void testDeleteTeam() {
        userTeamManagementService.deleteTeam(team);
        Mockito.verify(userTeamRelationRepository).findAllByTeamId("teamId");
        Mockito.verify(userTeamManagementService).deleteUserTeamRelation(Mockito.any());
        Mockito.verify(userTeamManagementService).sysTeamService.deleteTeam(team);
    }

}
