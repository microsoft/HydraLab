import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@RunWith(MockitoJUnitRunner.class)
public class SecurityConfigurationTest {

    @Mock
    private InMemoryUserDetailsManager userDetailsManager;

    @Test
    public void testUserDetailsService() {
        Mockito.when(userDetailsManager.loadUserByUsername(Mockito.anyString())).thenReturn(null);

        SecurityConfiguration securityConfiguration = new SecurityConfiguration();
        UserDetailsService userDetailsService = securityConfiguration.userDetailsService();

        // Perform assertions or verifications
    }
}