package com.microsoft.hydralab.center.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.springframework.security.core.session.SessionRegistry;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpSession;

@RunWith(MockitoJUnitRunner.class)
public class SecurityUserServiceTest {

    @Mock
    private SysUserService sysUserService;
    @Mock
    private SysRoleService sysRoleService;
    @Mock
    private SysTeamService sysTeamService;
    @Mock
    private AuthUtil authUtil;
    @Mock
    private RolePermissionManagementService rolePermissionManagementService;
    @Mock
    private UserTeamManagementService userTeamManagementService;
    @Mock
    private SessionManageService sessionManageService;
    @Mock
    private SessionRegistry sessionRegistry;
    private SecurityUserService securityUserService;
    @Mock
    private HttpSession session;
    @Mock
    private HttpSession mockSession;
    @Mock
    private SessionRegistry mockSessionRegistry;
    @Mock
    private Authentication mockAuthentication;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        securityUserService = new SecurityUserService();
    }

    @Test
    public void testLoadUserAuthentication() {
        String mailAddress = "test@example.com";
        String accessToken = "1234567890";
        SysUser user = new SysUser();
        user.setMailAddress(mailAddress);
        user.setAccessToken(accessToken);
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        when(sysUserService.queryUserByMailAddress(mailAddress)).thenReturn(user);
        when(sysRoleService.getOrCreateDefaultRole(anyString(), anyInt())).thenReturn(new SysRole());
        when(sysTeamService.getOrCreateDefaultTeam(anyString())).thenReturn(new SysTeam());
        when(userTeamManagementService.addUserTeamRelation(anyString(), any(SysUser.class), anyBoolean())).thenReturn(new UserTeamRelation());
        when(sysRoleService.queryRoleById(anyString())).thenReturn(new SysRole());
        when(rolePermissionManagementService.queryPermissionsByRole(anyString())).thenReturn(new ArrayList<>());
        Authentication result = securityUserService.loadUserAuthentication(mailAddress, accessToken);
        assertEquals(user, result);
        assertEquals(authorities, user.getAuthorities());
        verify(sysUserService, times(1)).queryUserByMailAddress(mailAddress);
        verify(sysRoleService, times(1)).getOrCreateDefaultRole(anyString(), anyInt());
        verify(sysTeamService, times(1)).getOrCreateDefaultTeam(anyString());
        verify(userTeamManagementService, times(1)).addUserTeamRelation(anyString(), any(SysUser.class), anyBoolean());
        verify(sysRoleService, times(1)).queryRoleById(anyString());
        verify(rolePermissionManagementService, times(1)).queryPermissionsByRole(anyString());
    }

    @Test
    public void testAddSessionAndUserAuth() {
        String mailAddress = "test@example.com";
        String accessToken = "123456";
        HttpSession session = mock(HttpSession.class);
        SysUser user = new SysUser();
        user.setMailAddress(mailAddress);
        user.setAccessToken(accessToken);
        when(sysUserService.queryUserByMailAddress(mailAddress)).thenReturn(null);
        when(authUtil.getLoginUserDisplayName(accessToken)).thenReturn("Test User");
        when(sysRoleService.getOrCreateDefaultRole(Const.DefaultRole.USER, 100)).thenReturn(new SysRole());
        when(sysTeamService.getOrCreateDefaultTeam(Const.DefaultTeam.DEFAULT_TEAM_NAME)).thenReturn(new SysTeam());
        when(sysUserService.createUserWithDefaultRole(anyString(), anyString(), anyInt(), anyString())).thenReturn(user);
        securityUserService.addSessionAndUserAuth(mailAddress, accessToken, session);
        verify(sessionManageService, times(1)).putUserSession(mailAddress, session);
        verify(sessionRegistry, times(1)).registerNewSession(session.getId(), mailAddress);
        verify(sysUserService, times(1)).queryUserByMailAddress(mailAddress);
        verify(authUtil, times(1)).getLoginUserDisplayName(accessToken);
        verify(sysRoleService, times(1)).getOrCreateDefaultRole(Const.DefaultRole.USER, 100);
        verify(sysTeamService, times(1)).getOrCreateDefaultTeam(Const.DefaultTeam.DEFAULT_TEAM_NAME);
        verify(sysUserService, times(1)).createUserWithDefaultRole(anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    public void testAddDefaultUserSession() {
        securityUserService.addDefaultUserSession(mockSession);
        verify(securityUserService.sessionManageService).putUserSession(eq("defaultUser"), eq(mockSession));
        verify(securityUserService).loadUserAuthentication(eq("defaultUser"), isNull());
        verify(mockSessionRegistry).registerNewSession(eq(mockSession.getId()), eq("defaultUser"));
    }

    @Test
    public void testReloadUserAuthentication() {
        String mailAddress = "test@example.com";
        String updateContent = "default_team";
        SysUser authUser = new SysUser();
        authUser.setMailAddress(mailAddress);
        authUser.setAccessToken("test_token");
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication instanceof SysUser).thenReturn(true);
        Mockito.when(authentication.getPrincipal()).thenReturn(authUser);
        Mockito.when(sessionManageService.getUserSessions(mailAddress)).thenReturn(new ArrayList<>());
        securityUserService.reloadUserAuthentication(mailAddress, updateContent);
        Mockito.verify(sessionManageService, Mockito.times(1)).getUserSessions(mailAddress);
        Mockito.verify(session, Mockito.never()).setAttribute(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testReloadUserAuthenticationToSession() {
        String updateContent = "updateContent";
        securityUserService.reloadUserAuthenticationToSession(session, updateContent);
        Mockito.verify(session, Mockito.times(1)).getAttribute(Mockito.eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        Mockito.verify(session, Mockito.times(1)).setAttribute(Mockito.eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY), Mockito.any());
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        securityUserService = new SecurityUserService();
        SecurityContextHolder.getContext().setAuthentication(mockAuthentication);
        securityUserService.sessionRegistry = mockSessionRegistry;
    }

}