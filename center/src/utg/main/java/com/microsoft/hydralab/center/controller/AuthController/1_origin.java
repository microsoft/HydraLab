import com.microsoft.hydralab.center.controller.AuthController;
import com.microsoft.hydralab.center.service.AuthTokenService;
import com.microsoft.hydralab.center.service.SecurityUserService;
import com.microsoft.hydralab.center.util.AuthUtil;
import com.microsoft.hydralab.center.util.SecretGenerator;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.AuthToken;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.util.Const;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.PathVariable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthControllerTest {
    @Mock
    private AuthUtil authUtil;
    @Mock
    private SecretGenerator secretGenerator;
    @Mock
    private AuthTokenService authTokenService;
    @Mock
    private SecurityUserService securityUserService;
    @InjectMocks
    private AuthController authController;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private SysUser sysUser;
    @Mock
    private Result result;
    @Mock
    private AuthToken authToken;
    @Mock
    private List<AuthToken> authTokens;

    @Before
    public void setup() {
        when(authUtil.verifyCode(any(String.class))).thenReturn("accessToken");
        when(authUtil.getLoginUrl()).thenReturn("loginUrl");
        when(authUtil.getLoginUserName(any(String.class))).thenReturn("userName");
        when(request.getParameter("state")).thenReturn("state");
        when(request.getSession()).thenReturn(null);
        when(authTokenService.getAuthToken(any(Long.class))).thenReturn(authToken);
        when(authToken.getCreator()).thenReturn("creator");
        when(sysUser.getMailAddress()).thenReturn("mailAddress");
        when(authTokenService.queryAuthToken()).thenReturn(authTokens);
        when(authTokenService.queryAuthTokenByName(any(String.class))).thenReturn(authTokens);
    }

    @Test
    public void testDeleteToken() {
        when(authTokenService.deleteAuthToken(any(AuthToken.class))).thenReturn(result);
        Result actualResult = authController.deleteToken(sysUser, 1L);
        assertEquals(result, actualResult);
    }
}