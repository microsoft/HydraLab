import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

@RunWith(MockitoJUnitRunner.class)
public class AppConfigTest {
@Mock
private SessionRegistry sessionRegistry;
@Test
public void testSessionRegistry() {
 SessionRegistryImpl sessionRegistryImpl = new SessionRegistryImpl(); 
}

}
