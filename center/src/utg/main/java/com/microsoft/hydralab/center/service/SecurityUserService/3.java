import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class SecurityUserServiceTest {
@Mock
private HttpSession session;
@Mock
private SessionManageService sessionManageService;
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
private SessionRegistry sessionRegistry;
@InjectMocks
private SecurityUserService securityUserService;
@Before
public void setup() {
 SecurityContext securityContext = Mockito.mock(SecurityContext.class); Mockito.when(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)) .thenReturn(securityContext); SecurityContextHolder.setContext(securityContext); 
}

@Test
public void testReloadUserAuthentication() {
 String mailAddress = "test@example.com"; String updateContent = "default_team"; SysUser authUser = new SysUser(); authUser.setMailAddress(mailAddress); authUser.setAccessToken("test_token"); Authentication authentication = Mockito.mock(Authentication.class); Mockito.when(authentication instanceof SysUser).thenReturn(true); Mockito.when(authentication.getPrincipal()).thenReturn(authUser); Mockito.when(sessionManageService.getUserSessions(mailAddress)).thenReturn(new ArrayList<>()); securityUserService.reloadUserAuthentication(mailAddress, updateContent); Mockito.verify(sessionManageService, Mockito.times(1)).getUserSessions(mailAddress); Mockito.verify(session, Mockito.never()).setAttribute(Mockito.anyString(), Mockito.any()); 
}

}
