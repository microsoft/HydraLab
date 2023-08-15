import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@RunWith(MockitoJUnitRunner.class)
public class SecurityConfigurationTest {
@Mock
private BCryptPasswordEncoder bCryptPasswordEncoder;
@Test
public void testPasswordEncoder() {
 PasswordEncoder passwordEncoder = Mockito.mock(BCryptPasswordEncoder.class); SecurityConfiguration securityConfiguration = new SecurityConfiguration(); Mockito.when(passwordEncoder.encode(Mockito.anyString())).thenReturn("encodedPassword"); PasswordEncoder result = securityConfiguration.passwordEncoder(); Mockito.verify(passwordEncoder).encode(Mockito.anyString()); Mockito.verifyNoMoreInteractions(passwordEncoder); 
}

}
