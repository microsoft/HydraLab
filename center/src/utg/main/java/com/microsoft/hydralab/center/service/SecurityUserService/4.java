import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import javax.servlet.http.HttpSession;

public class SecurityUserServiceTest {
@Mock
private HttpSession session;
private SecurityUserService securityUserService;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); securityUserService = new SecurityUserService(); 
}

@Test
public void testReloadUserAuthenticationToSession() {
 String updateContent = "updateContent"; securityUserService.reloadUserAuthenticationToSession(session, updateContent); Mockito.verify(session, Mockito.times(1)).getAttribute(Mockito.eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)); Mockito.verify(session, Mockito.times(1)).setAttribute(Mockito.eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY), Mockito.any()); 
}

}
