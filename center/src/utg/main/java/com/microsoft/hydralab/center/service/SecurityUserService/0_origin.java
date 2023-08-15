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

    @InjectMocks
    private SecurityUserService securityUserService;

    @Mock
    private HttpSession session;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testLoadUserAuthentication() {
        // Arrange
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

        // Act
        Authentication result = securityUserService.loadUserAuthentication(mailAddress, accessToken);

        // Assert
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
        // Arrange
        String mailAddress = "test@example.com";
        String accessToken = "1234567890";
        Authentication authObj = mock(Authentication.class);
        when(securityUserService.loadUserAuthentication(mailAddress, accessToken)).thenReturn(authObj);

        // Act
        securityUserService.addSessionAndUserAuth(mailAddress, accessToken, session);

        // Assert
        verify(sessionManageService, times(1)).putUserSession(mailAddress, session);
        verify(securityUserService, times(1)).loadUserAuthentication(mailAddress, accessToken);
        verify(sessionRegistry, times(1)).registerNewSession(session.getId(), mailAddress);
        assertEquals(authObj, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testAddDefaultUserSession() {
        // Arrange
        Authentication authObj = mock(Authentication.class);
        when(securityUserService.loadUserAuthentication(anyString(), anyString())).thenReturn(authObj);

        // Act
        securityUserService.addDefaultUserSession(session);

        // Assert
        verify(sessionManageService, times(1)).putUserSession(anyString(), eq(session));
        verify(securityUserService, times(1)).loadUserAuthentication(anyString(), anyString());
        verify(sessionRegistry, times(1)).registerNewSession(session.getId(), anyString());
        assertEquals(authObj, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testReloadUserAuthentication() {
        // Arrange
        String mailAddress = "test@example.com";
        String updateContent = "default_team";
        HttpSession session1 = mock(HttpSession.class);
        HttpSession session2 = mock(HttpSession.class);
        List<HttpSession> sessions = new ArrayList<>();
        sessions.add(session1);
        sessions.add(session2);
        when(sessionManageService.getUserSessions(mailAddress)).thenReturn(sessions);

        // Act
        securityUserService.reloadUserAuthentication(mailAddress, updateContent);

        // Assert
        verify(sessionManageService, times(1)).getUserSessions(mailAddress);
        verify(securityUserService, times(1)).reloadUserAuthenticationToSession(session1, updateContent);
        verify(securityUserService, times(1)).reloadUserAuthenticationToSession(session2, updateContent);
    }

    @Test
    public void testReloadUserAuthenticationToSession() {
        // Arrange
        String updateContent = "default_team";
        HttpSession session = mock(HttpSession.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        SysUser authUser = mock(SysUser.class);
        when(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication instanceof SysUser).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(authUser);

        // Act
        securityUserService.reloadUserAuthenticationToSession(session, updateContent);

        // Assert
        verify(session, times(1)).getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        verify(securityContext, times(1)).getAuthentication();
        verify(authentication, times(1)).getPrincipal();
        verify(authUser, times(1)).getMailAddress();
        verify(authUser, times(1)).getAccessToken();
        verify(securityUserService, times(1)).loadUserAuthentication(authUser.getMailAddress(), authUser.getAccessToken());
        verify(securityContext, times(1)).setAuthentication(any(Authentication.class));
    }
}