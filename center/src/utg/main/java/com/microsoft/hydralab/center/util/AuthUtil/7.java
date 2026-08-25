import com.microsoft.hydralab.center.util.AuthUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthUtilTest {
@Mock
private RestTemplate restTemplate;
@Test
public void testVerifyCode() {
 String code = "testCode"; String clientId = "testClientId"; String redirectUri = "testRedirectUri"; String clientSecret = "testClientSecret"; String tokenUrl = "testTokenUrl"; String accessToken = "testAccessToken"; HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>(); body.add("client_id", clientId); body.add("code", code); body.add("redirect_uri", redirectUri); body.add("grant_type", "authorization_code"); body.add("client_secret", clientSecret); HttpEntity<LinkedMultiValueMap<String, String>> entity = new HttpEntity<>(body, headers); ResponseEntity<JSONObject> responseEntity = new ResponseEntity<>(HttpStatus.OK); when(restTemplate.exchange(eq(tokenUrl), eq(HttpMethod.POST), eq(entity), eq(JSONObject.class))) .thenReturn(responseEntity); AuthUtil authUtil = new AuthUtil(); authUtil.tokenUrl = tokenUrl; authUtil.clientId = clientId; authUtil.redirectUri = redirectUri; authUtil.clientSecret = clientSecret; String result = authUtil.verifyCode(code); assertEquals(accessToken, result); verify(restTemplate, times(1)).exchange(eq(tokenUrl), eq(HttpMethod.POST), eq(entity), eq(JSONObject.class)); 
}

}
