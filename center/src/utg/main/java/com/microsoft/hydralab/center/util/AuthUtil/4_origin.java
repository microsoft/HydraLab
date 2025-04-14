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
    public void testGetLoginUserDisplayName() {
        String accessToken = "sampleAccessToken";
        String expectedDisplayName = "John Doe";

        Mockito.when(authUtil.decodeAccessToken(accessToken)).thenReturn(getMockUserInfo(expectedDisplayName));

        String displayName = authUtil.getLoginUserDisplayName(accessToken);

        assertEquals(expectedDisplayName, displayName);
    }

    private JSONObject getMockUserInfo(String displayName) {
        JSONObject userInfo = new JSONObject();
        userInfo.put("name", displayName);
        return userInfo;
    }
}