import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;

public class AuthUtilTest {

    private AuthUtil authUtil;

    @Before
    public void setup() {
        authUtil = new AuthUtil();
    }

    @Test
    public void testDecodeAccessToken() {
        // Arrange
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        // Act
        JSONObject result = authUtil.decodeAccessToken(accessToken);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals("1234567890", result.getString("sub"));
        Assert.assertEquals("John Doe", result.getString("name"));
        Assert.assertEquals(1516239022, result.getLong("iat"));
    }
}