import com.microsoft.hydralab.center.util.AuthUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class AuthUtilTest {
@Test
public void testIsIgnore() {
 AuthUtil authUtil = new AuthUtil(); String requestUrl = "example.com"; ReflectionTestUtils.setField(authUtil, "urlMapping", Mockito.mock(Map.class)); Mockito.when(authUtil.urlMapping.get(requestUrl)).thenReturn(true); boolean result = authUtil.isIgnore(requestUrl); Assert.assertTrue(result); 
}

}
