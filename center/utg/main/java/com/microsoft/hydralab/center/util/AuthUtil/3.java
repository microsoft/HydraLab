import com.microsoft.hydralab.center.util.AuthUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AuthUtilTest {
@Test
public void testGetLoginUserName() {
 AuthUtil authUtil = new AuthUtil(); String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1bmlxdWVfbmFtZSI6ImFkbWluIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"; JSONObject mockUserInfo = Mockito.mock(JSONObject.class); Mockito.when(mockUserInfo.getString("unique_name")).thenReturn("admin"); Mockito.when(authUtil.decodeAccessToken(Mockito.anyString())).thenReturn(mockUserInfo); String result = authUtil.getLoginUserName(accessToken); Assert.assertEquals("admin", result); 
}

}
