import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;

import javax.servlet.http.HttpSession;

import static org.mockito.Mockito.*;

public class SecurityUserServiceTest {

    @Mock
    private HttpSession mockSession;

    @Mock
    private SessionRegistry mockSessionRegistry;

    @Mock
    private Authentication mockAuthentication;

    private SecurityUserService securityUserService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        securityUserService = new SecurityUserService();
        SecurityContextHolder.getContext().setAuthentication(mockAuthentication);
        securityUserService.sessionRegistry = mockSessionRegistry;
    }

    @Test
    public void testAddDefaultUserSession() {
        securityUserService.addDefaultUserSession(mockSession);
        verify(securityUserService.sessionManageService).putUserSession(eq("defaultUser"), eq(mockSession));
        verify(securityUserService).loadUserAuthentication(eq("defaultUser"), isNull());
        verify(mockSessionRegistry).registerNewSession(eq(mockSession.getId()), eq("defaultUser"));
    }
}