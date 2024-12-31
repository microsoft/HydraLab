import com.microsoft.hydralab.center.util.AuthUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class AuthUtilTest {

    @Test
    public void testIsIgnore() {
        AuthUtil authUtil = new AuthUtil();
        String requestUrl = "example.com";
        
        // Mock the urlMapping map
        ReflectionTestUtils.setField(authUtil, "urlMapping", Mockito.mock(Map.class));
        
        // Mock the urlMapping.get() method to return true
        Mockito.when(authUtil.urlMapping.get(requestUrl)).thenReturn(true);
        
        // Call the target function
        boolean result = authUtil.isIgnore(requestUrl);
        
        // Verify the result
        Assert.assertTrue(result);
    }
}