import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;

import javax.servlet.http.HttpSession;

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

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
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
}