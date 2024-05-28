import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AuthUtilTest {

    @Mock
    private AuthUtil authUtil;

    @Test
    public void testGetLoginUrl() {
        String originUrl = "https://example.com";
        String queryString = "param1=value1&param2=value2";

        String expectedLoginUrl = "https://example.com?param1=value1&param2=value2";

        Mockito.when(authUtil.getLoginUrl(originUrl, queryString)).thenReturn(expectedLoginUrl);

        String actualLoginUrl = authUtil.getLoginUrl(originUrl, queryString);

        assertEquals(expectedLoginUrl, actualLoginUrl);
    }
}