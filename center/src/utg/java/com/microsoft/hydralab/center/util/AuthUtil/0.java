import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthUtilTest {
@InjectMocks
private AuthUtil authUtil;
@Test
public void testIsIgnore_NullRequestUrl_ReturnsFalse() {
 boolean result = authUtil.isIgnore(null); assertFalse(result); 
}

@Test
public void testIsIgnore_UrlMappingIsNull_ReturnsFalse() {
 when(urlMapping.get(anyString())).thenReturn(null); boolean result = authUtil.isIgnore("example.com"); assertFalse(result); verify(urlMapping, times(1)).get(anyString()); 
}

@Test
public void testIsIgnore_UrlMappingContainsRequestUrl_ReturnsTrue() {
 when(urlMapping.get(anyString())).thenReturn(true); boolean result = authUtil.isIgnore("example.com"); assertTrue(result); verify(urlMapping, times(1)).get(anyString()); 
}

@Test
public void testIsIgnore_UrlMappingDoesNotContainRequestUrl_ReturnsFalse() {
 when(urlMapping.get(anyString())).thenReturn(false); boolean result = authUtil.isIgnore("example.com"); assertFalse(result); verify(urlMapping, times(1)).get(anyString()); 
}

@Test
public void testVerifyToken_ValidToken_ReturnsTrue() {
 String token = "validToken"; boolean result = authUtil.verifyToken(token); assertTrue(result); 
}

@Test
public void testVerifyToken_InvalidToken_ReturnsFalse() {
 String token = "invalidToken"; boolean result = authUtil.verifyToken(token); assertFalse(result); 
}

@Test
public void testDecodeAccessToken_ValidAccessToken_ReturnsUserInfo() {
 String accessToken = "validAccessToken"; JSONObject userInfo = authUtil.decodeAccessToken(accessToken); assertNotNull(userInfo); 
}

@Test
public void testDecodeAccessToken_InvalidAccessToken_ReturnsNull() {
 String accessToken = "invalidAccessToken"; JSONObject userInfo = authUtil.decodeAccessToken(accessToken); assertNull(userInfo); 
}

@Test
public void testGetLoginUserName_ValidAccessToken_ReturnsUsername() {
 String accessToken = "validAccessToken"; String username = authUtil.getLoginUserName(accessToken); assertNotNull(username); 
}

@Test
public void testGetLoginUserName_InvalidAccessToken_ReturnsEmptyString() {
 String accessToken = "invalidAccessToken"; String username = authUtil.getLoginUserName(accessToken); assertEquals("", username); 
}

@Test
public void testGetLoginUserDisplayName_ValidAccessToken_ReturnsName() {
 String accessToken = "validAccessToken"; String name = authUtil.getLoginUserDisplayName(accessToken); assertNotNull(name); 
}

@Test
public void testGetLoginUserDisplayName_InvalidAccessToken_ReturnsEmptyString() {
 String accessToken = "invalidAccessToken"; String name = authUtil.getLoginUserDisplayName(accessToken); assertEquals("", name); 
}

@Test
public void testGetLoginUrl_ReturnsLoginUrl() {
 String loginUrl = authUtil.getLoginUrl(); assertNotNull(loginUrl); 
}

@Test
public void testGetLoginUrl_WithOriginUrlAndQueryString_ReturnsLoginUrl() {
 String originUrl = "example.com"; String queryString = "param=value"; String loginUrl = authUtil.getLoginUrl(originUrl, queryString); assertNotNull(loginUrl); 
}

@Test
public void testVerifyCode_ValidCode_ReturnsAccessToken() {
 String code = "validCode"; String accessToken = authUtil.verifyCode(code); assertNotNull(accessToken); 
}

@Test
public void testVerifyCode_InvalidCode_ReturnsNull() {
 String code = "invalidCode"; String accessToken = authUtil.verifyCode(code); assertNull(accessToken); 
}

@Test
public void testRequestPhoto_ValidAccessToken_ReturnsInputStream() {
 String accessToken = "validAccessToken"; InputStream inputStream = authUtil.requestPhoto(accessToken); assertNotNull(inputStream); 
}

public void testRequestPhoto_InvalidAccessToken_ThrowsException() {
 String accessToken = "invalidAccessToken"; authUtil.requestPhoto(accessToken); 
}

}
