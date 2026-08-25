import com.microsoft.hydralab.center.util.AuthUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthUtilTest {
    @Mock
    private AuthUtil authUtil;

    @Test
    public void testGetLoginUrl() {
        String authorizationUri = "https://example.com/auth";
        String clientId = "123456789";
        String redirectUri = "https://example.com/callback";
        String scope = "openid profile";
        String expectedLoginUrl = authorizationUri + "?client_id=" + clientId + "&response_type=code&redirect_uri=" + redirectUri + "&response_mode=query&scope=" + scope;

        when(authUtil.getLoginUrl()).thenReturn(expectedLoginUrl);

        String actualLoginUrl = authUtil.getLoginUrl();

        assertEquals(expectedLoginUrl, actualLoginUrl);
    }
}