import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.mockito.Mockito;
import org.junit.Test;

public class AADOauth2ConfigurationTest {
public void testConfigure() {
", " " AADOauth2Configuration configuration = new AADOauth2Configuration();", " HttpSecurity http = Mockito.mock(HttpSecurity.class);", "", " " configuration.configure(http);", "", " " " 
}

}
