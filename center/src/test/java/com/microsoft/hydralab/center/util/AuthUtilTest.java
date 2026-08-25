package com.microsoft.hydralab.center.util;

import com.microsoft.hydralab.center.util.AuthUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import com.alibaba.fastjson.JSONObject;
import org.junit.Before;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.Base64Utils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import static org.mockito.Mockito.*;
import org.springframework.core.io.Resource;

@RunWith(MockitoJUnitRunner.class)
public class AuthUtilTest {

    @Mock
    private AuthUtil authUtil;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private HttpHeaders headers;
    @Mock
    private ResponseEntity<Resource> responseEntity;
    @Mock
    private Resource resource;

    @Test
    public void testIsIgnore() {
        AuthUtil authUtil = new AuthUtil();
        String requestUrl = "example.com";
        ReflectionTestUtils.setField(authUtil, "urlMapping", Mockito.mock(Map.class));
        Mockito.when(authUtil.urlMapping.get(requestUrl)).thenReturn(true);
        boolean result = authUtil.isIgnore(requestUrl);
        Assert.assertTrue(result);
    }

    @Before
    public void setup() {
        authUtil = new AuthUtil();
    }

    @Test
    public void testVerifyToken() {
        String token = "sampleToken";
        JSONObject userInfo = new JSONObject();
        userInfo.put("appid", authUtil.clientId);
        Mockito.when(restTemplate.exchange(Mockito.anyString(), Mockito.eq(HttpMethod.POST), Mockito.any(HttpEntity.class), Mockito.eq(JSONObject.class)))
                .thenReturn(ResponseEntity.ok(userInfo));
        boolean result = authUtil.verifyToken(token);
        Assert.assertTrue(result);
    }

    @Test
    public void testDecodeAccessToken() {
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        JSONObject result = authUtil.decodeAccessToken(accessToken);
        Assert.assertNotNull(result);
        Assert.assertEquals("1234567890", result.getString("sub"));
        Assert.assertEquals("John Doe", result.getString("name"));
        Assert.assertEquals(1516239022, result.getLong("iat"));
    }

    @Test
    public void testGetLoginUserName() {
        AuthUtil authUtil = new AuthUtil();
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1bmlxdWVfbmFtZSI6ImFkbWluIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        JSONObject mockUserInfo = Mockito.mock(JSONObject.class);
        Mockito.when(mockUserInfo.getString("unique_name")).thenReturn("admin");
        Mockito.when(authUtil.decodeAccessToken(Mockito.anyString())).thenReturn(mockUserInfo);
        String result = authUtil.getLoginUserName(accessToken);
        Assert.assertEquals("admin", result);
    }

    @Test
    public void testGetLoginUserDisplayName() {
        String accessToken = "sampleAccessToken";
        String expectedDisplayName = "John Doe";
        Mockito.when(authUtil.decodeAccessToken(accessToken)).thenReturn(getMockUserInfo(expectedDisplayName));
        String displayName = authUtil.getLoginUserDisplayName(accessToken);
        assertEquals(expectedDisplayName, displayName);
    }

    @Test
    public void testGetLoginUrl() {
        String originUrl = "https:";
        String queryString = "param1=value1&param2=value2";
        String expectedLoginUrl = "https:";
        Mockito.when(authUtil.getLoginUrl(originUrl, queryString)).thenReturn(expectedLoginUrl);
        String actualLoginUrl = authUtil.getLoginUrl(originUrl, queryString);
        assertEquals(expectedLoginUrl, actualLoginUrl);
    }

    @Test
    public void testVerifyCode() {
        String code = "testCode";
        String clientId = "testClientId";
        String redirectUri = "testRedirectUri";
        String clientSecret = "testClientSecret";
        String tokenUrl = "testTokenUrl";
        String accessToken = "testAccessToken";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");
        body.add("client_secret", clientSecret);
        HttpEntity<LinkedMultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<JSONObject> responseEntity = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.exchange(eq(tokenUrl), eq(HttpMethod.POST), eq(entity), eq(JSONObject.class)))
                .thenReturn(responseEntity);
        AuthUtil authUtil = new AuthUtil();
        authUtil.tokenUrl = tokenUrl;
        authUtil.clientId = clientId;
        authUtil.redirectUri = redirectUri;
        authUtil.clientSecret = clientSecret;
        String result = authUtil.verifyCode(code);
        assertEquals(accessToken, result);
        verify(restTemplate, times(1)).exchange(eq(tokenUrl), eq(HttpMethod.POST), eq(entity), eq(JSONObject.class));
    }

    @Test
    public void testRequestPhoto() {
        String accessToken = "testAccessToken";
        InputStream expectedInputStream = mock(InputStream.class);
        AuthUtil authUtil = new AuthUtil();
        authUtil.photoUrl = "testPhotoUrl";
        when(restTemplate.exchange(eq(authUtil.photoUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(Resource.class)))
                .thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(expectedInputStream);
        InputStream actualInputStream = authUtil.requestPhoto(accessToken);
        assertEquals(expectedInputStream, actualInputStream);
        verify(headers).add("Authorization", "Bearer " + accessToken);
        verify(restTemplate).exchange(eq(authUtil.photoUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(Resource.class));
    }

}