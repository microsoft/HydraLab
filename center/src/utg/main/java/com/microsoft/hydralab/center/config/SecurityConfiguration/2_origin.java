import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@RunWith(MockitoJUnitRunner.class)
public class SecurityConfigurationTest {

    @Mock
    private InMemoryUserDetailsManager userDetailsManager;

    @Test
    public void testAuthenticationManagerBean() throws Exception {
        SecurityConfiguration securityConfiguration = new SecurityConfiguration();

        Mockito.when(userDetailsManager.createUser(Mockito.any())).thenReturn(null);
        Mockito.when(userDetailsManager.loadUserByUsername(Mockito.anyString())).thenReturn(null);

        AuthenticationManager authenticationManager = securityConfiguration.authenticationManagerBean();

        // Perform assertions or verifications
    }
}