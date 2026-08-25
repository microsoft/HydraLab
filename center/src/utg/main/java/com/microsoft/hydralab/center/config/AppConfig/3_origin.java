import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@RunWith(MockitoJUnitRunner.class)
public class AppConfigTest {

    @Mock
    private HttpSessionEventPublisher httpSessionEventPublisher;

    @Test
    public void testEventPublisher() {
        AppConfig appConfig = new AppConfig();
        HttpSessionEventPublisher eventPublisher = appConfig.eventPublisher();
        // Perform assertions or verifications
    }
}