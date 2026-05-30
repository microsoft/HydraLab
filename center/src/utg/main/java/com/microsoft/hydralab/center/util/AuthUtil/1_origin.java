import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class AuthUtilTest {

    private AuthUtil authUtil;
    private RestTemplate restTemplate;

    @Before
    public void setup() {
        authUtil = new AuthUtil();
        restTemplate = Mockito.mock(RestTemplate.class);
    }

    @Test
    public void testVerifyToken() {
        // Arrange
        String token = "sampleToken";
        JSONObject userInfo = new JSONObject();
        userInfo.put("appid", authUtil.clientId);

        Mockito.when(restTemplate.exchange(Mockito.anyString(), Mockito.eq(HttpMethod.POST), Mockito.any(HttpEntity.class), Mockito.eq(JSONObject.class)))
                .thenReturn(ResponseEntity.ok(userInfo));

        // Act
        boolean result = authUtil.verifyToken(token);

        // Assert
        Assert.assertTrue(result);
    }
}