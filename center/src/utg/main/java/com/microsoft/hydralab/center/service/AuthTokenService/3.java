import com.microsoft.hydralab.center.repository.AuthTokenRepository;
import com.microsoft.hydralab.common.entity.center.AuthToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AuthTokenServiceTest {
@Mock
private AuthTokenRepository authTokenRepository;
@Mock
private SecurityUserService securityUserService;
private AuthTokenService authTokenService;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); authTokenService = new AuthTokenService(); authTokenService.authTokenRepository = authTokenRepository; authTokenService.securityUserService = securityUserService; 
}

@Test
public void testCheckAuthToken() {
 String authToken = "token"; List<AuthToken> authTokens = new ArrayList<>(); AuthToken token = new AuthToken(); token.setToken(authToken); authTokens.add(token); Mockito.when(authTokenRepository.queryByToken(authToken)).thenReturn(authTokens); Authentication authObj = Mockito.mock(Authentication.class); Mockito.when(securityUserService.loadUserAuthentication(Mockito.anyString(), Mockito.isNull())).thenReturn(authObj); boolean result = authTokenService.checkAuthToken(authToken); Mockito.verify(authTokenRepository).queryByToken(authToken); Mockito.verify(securityUserService).loadUserAuthentication(Mockito.anyString(), Mockito.isNull()); Mockito.verify(SecurityContextHolder.getContext()).setAuthentication(authObj); Assert.assertTrue(result); 
}

}
