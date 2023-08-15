import com.microsoft.hydralab.center.controller.AuthController;
import com.microsoft.hydralab.center.service.AuthTokenService;
import com.microsoft.hydralab.center.service.SecurityUserService;
import com.microsoft.hydralab.center.util.AuthUtil;
import com.microsoft.hydralab.center.util.SecretGenerator;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.AuthToken;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.util.Const;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthControllerTest {
    @Mock
    AuthUtil authUtil;
    @Mock
    SecretGenerator secretGenerator;
    @Mock
    AuthTokenService authTokenService;
    @Mock
    SecurityUserService securityUserService;
    @InjectMocks
    AuthController authController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetUserPhoto() throws IOException {
        // Arrange
        SysUser sysUser = new SysUser();
        HttpServletResponse response = mock(HttpServletResponse.class);
        InputStream inputStream = mock(InputStream.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(authUtil.requestPhoto(anyString())).thenReturn(inputStream);
        when(response.getOutputStream()).thenReturn(outputStream);

        // Act
        authController.getUserPhoto(sysUser, response);

        // Assert
        verify(authUtil, times(1)).requestPhoto(anyString());
        verify(response, times(1)).setContentType(MediaType.IMAGE_JPEG_VALUE);
        verify(outputStream, times(1)).write(any(byte[].class), anyInt(), anyInt());
        verify(outputStream, times(1)).flush();
        verify(outputStream, times(1)).close();
    }
}