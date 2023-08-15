import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class SessionManageServiceTest {
private SessionManageService sessionManageService;
@Mock
private SessionRegistry sessionRegistry;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); sessionManageService = new SessionManageService(); sessionManageService.sessionRegistry = sessionRegistry; 
}

@Test
public void testPutUserSession() {
 String mailAddress = "test@example.com"; HttpSession httpSession = mock(HttpSession.class); SessionInformation sessionInformation = mock(SessionInformation.class); List<HttpSession> userSessions = new ArrayList<>(); userSessions.add(httpSession); when(sessionRegistry.getAllSessions(mailAddress, false)).thenReturn(new ArrayList<>()); when(httpSession.getId()).thenReturn("12345"); when(sessionInformation.getSessionId()).thenReturn("12345"); sessionManageService.putUserSession(mailAddress, httpSession); verify(sessionRegistry, times(1)).getAllSessions(mailAddress, false); verify(httpSession, times(1)).getId(); verify(sessionInformation, times(1)).getSessionId(); assertEquals(userSessions, sessionManageService.getUserSessions(mailAddress)); 
}

}
