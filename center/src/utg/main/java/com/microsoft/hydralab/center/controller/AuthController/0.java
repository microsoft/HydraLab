import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthControllerTest {
@Mock
private AuthUtil authUtil;
@Mock
private HttpServletRequest request;
@Mock
private HttpServletResponse response;
private AuthController authController;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); authController = new AuthController(); authController.authUtil = authUtil; 
}

@Test
public void testGetAccessToken() {
 String code = "testCode"; String redirectUrl = "testRedirectUrl"; String accessToken = "testAccessToken"; Mockito.when(authUtil.verifyCode(code)).thenReturn(accessToken); Mockito.when(request.getParameter("state")).thenReturn(redirectUrl); authController.getAccessToken(code, request, response); Mockito.verify(response).sendRedirect(redirectUrl); 
}

}
