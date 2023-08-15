import com.microsoft.hydralab.center.util.AuthUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.io.InputStream;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthUtilTest {
@Mock
private RestTemplate restTemplate;
@Mock
private HttpHeaders headers;
@Mock
private ResponseEntity<Resource> responseEntity;
@Mock
private Resource resource;
@Test
public void testRequestPhoto() {
 String accessToken = "testAccessToken"; InputStream expectedInputStream = mock(InputStream.class); AuthUtil authUtil = new AuthUtil(); authUtil.photoUrl = "testPhotoUrl"; when(restTemplate.exchange(eq(authUtil.photoUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(Resource.class))) .thenReturn(responseEntity); when(responseEntity.getBody()).thenReturn(resource); when(resource.getInputStream()).thenReturn(expectedInputStream); InputStream actualInputStream = authUtil.requestPhoto(accessToken); assertEquals(expectedInputStream, actualInputStream); verify(headers).add("Authorization", "Bearer " + accessToken); verify(restTemplate).exchange(eq(authUtil.photoUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(Resource.class)); 
}

}
