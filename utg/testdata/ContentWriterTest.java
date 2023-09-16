import com.microsoft.hydralab.center.util.AuthUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import com.alibaba.fastjson.JSONObject;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import java.io.InputStream;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.common.util.RestTemplateConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.util.LinkedMultiValueMap;
import static org.junit.Assert.*;

public class AuthUtilTest {

  @InjectMocks
  private AuthUtil authUtil;
  @Mock
  private JSONObject userInfo;
  @Mock
  private JSONObject jsonObject;
  @Mock
  private RestTemplate restTemplate;
  @Mock
  private Resource resource;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testIsIgnore_WithNullRequestUrl_ReturnsFalse() {
    String requestUrl = null;
    boolean result = authUtil.isIgnore(requestUrl);
    assertFalse(result);
  }

  @Test
  public void testIsIgnore_WithNonIgnoredRequestUrl_ReturnsFalse() {
    String requestUrl = "https://example.com";
    when(urlMapping.get(requestUrl)).thenReturn(null);
    boolean result = authUtil.isIgnore(requestUrl);
    assertFalse(result);
  }

  @Test
  public void testIsIgnore_WithIgnoredRequestUrl_ReturnsTrue() {
    String requestUrl = "https://example.com";
    when(urlMapping.get(requestUrl)).thenReturn(true);
    boolean result = authUtil.isIgnore(requestUrl);
    assertTrue(result);
  }

  @Test
  public void testVerifyTokenWithValidToken() {
    String token = "validToken";
    when(authUtil.decodeAccessToken(token)).thenReturn(userInfo);
    when(userInfo.getString("appid")).thenReturn("clientId");
    boolean result = authUtil.verifyToken(token);
    assertTrue(result);
  }

  @Test
  public void testVerifyTokenWithInvalidToken() {
    String token = "invalidToken";
    when(authUtil.decodeAccessToken(token)).thenReturn(userInfo);
    when(userInfo.getString("appid")).thenReturn("differentClientId");
    boolean result = authUtil.verifyToken(token);
    assertFalse(result);
  }

  @Test
  public void testDecodeAccessToken() {
    String accessToken = "sampleAccessToken";
    String expectedJsonString = "{\"name\":\"John Doe\",\"appid\":\"1234567890\",\"unique_name\":\"johndoe\"}";
  }

  @Test
  public void testGetLoginUserName() {
    String accessToken = "sampleAccessToken";
    String expectedUsername = "sampleUsername";
    JSONObject userInfo = new JSONObject();
    userInfo.put("unique_name", expectedUsername);
    when(authUtil.decodeAccessToken(accessToken)).thenReturn(userInfo);
    String username = authUtil.getLoginUserName(accessToken);
    assertEquals(expectedUsername, username);
  }

  @Test
  public void testGetLoginUserDisplayName() {
    String accessToken = "testAccessToken";
    String expectedName = "John Doe";
    JSONObject userInfo = new JSONObject();
    userInfo.put("name", expectedName);
    when(authUtil.decodeAccessToken(accessToken)).thenReturn(userInfo);
    String displayName = authUtil.getLoginUserDisplayName(accessToken);
    assertEquals(expectedName, displayName);
  }

  @Test
  public void testGetLoginUrl() {
    MockitoAnnotations.initMocks(this);
    ReflectionTestUtils.setField(authUtil, "authorizationUri", "https://example.com/auth");
    ReflectionTestUtils.setField(authUtil, "clientId", "client123");
    ReflectionTestUtils.setField(authUtil, "redirectUri", "https://example.com/callback");
    ReflectionTestUtils.setField(authUtil, "scope", "openid profile");
    String expectedUrl = "https://example.com/auth?client_id=client123&response_type=code&redirect_uri=https%3A%2F%2Fexample.com%2Fcallback&response_mode=query&scope=openid%20profile";
    String loginUrl = authUtil.getLoginUrl();
    assertEquals(expectedUrl, loginUrl);
  }

  @Test
  public void testVerifyCode() {
    String code = "testCode";
    String expectedAccessToken = "testAccessToken";
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/x-www-form-urlencoded");
    LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("client_id", authUtil.getClientId());
    body.add("code", code);
    body.add("redirect_uri", authUtil.getRedirectUri());
    body.add("grant_type", "authorization_code");
    body.add("client_secret", authUtil.getClientSecret());
    HttpEntity<LinkedMultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
    JSONObject json = new JSONObject();
    json.put("access_token", expectedAccessToken);
    ResponseEntity<JSONObject> responseEntity = ResponseEntity.ok(json);
    when(restTemplate.exchange(eq(authUtil.getTokenUrl()), eq(HttpMethod.POST), eq(entity), eq(JSONObject.class)))
        .thenReturn(responseEntity);
    String accessToken = authUtil.verifyCode(code);
    assertEquals(expectedAccessToken, accessToken);
  }

  @Test
  public void testVerifyCode() {
    String code = "testCode";
    String accessToken = "testAccessToken";
    String tokenUrl = "testTokenUrl";
    String clientId = "testClientId";
    String redirectUri = "testRedirectUri";
    String clientSecret = "testClientSecret";
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/x-www-form-urlencoded");
    LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("client_id", clientId);
    body.add("code", code);
    body.add("redirect_uri", redirectUri);
    body.add("grant_type", "authorization_code");
    body.add("client_secret", clientSecret);
    HttpEntity<LinkedMultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
    JSONObject jsonBody = new JSONObject();
    jsonBody.put("access_token", accessToken);
    ResponseEntity<JSONObject> responseEntity = ResponseEntity.ok(jsonBody);
    when(restTemplate.exchange(eq(tokenUrl), eq(HttpMethod.POST), eq(entity), eq(JSONObject.class)))
        .thenReturn(responseEntity);
    String result = authUtil.verifyCode(code);
    assertEquals(accessToken, result);
  }
}