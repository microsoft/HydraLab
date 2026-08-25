import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BaseInterceptorTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Object handler;

    private BaseInterceptor baseInterceptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        baseInterceptor = new BaseInterceptor();
    }

    @Test
    public void testPreHandle() throws IOException {
        // Arrange
        when(request.getRemoteUser()).thenReturn("user");
        when(request.getRequestURI()).thenReturn("/path");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRemoteHost()).thenReturn("localhost");
        when(baseInterceptor.isLegalStr(anyString(), anyString(), anyBoolean())).thenReturn(true);
        when(baseInterceptor.isIgnore(anyString())).thenReturn(false);
        when(baseInterceptor.verifyToken(anyString())).thenReturn(true);
        when(baseInterceptor.getLoginUrl()).thenReturn("http://localhost/login");

        // Act
        boolean result = baseInterceptor.preHandle(request, response, handler);

        // Assert
        assertTrue(result);
        verify(request, times(1)).getRemoteUser();
        verify(request, times(1)).getRequestURI();
        verify(request, times(1)).getRemoteAddr();
        verify(request, times(1)).getRemoteHost();
        verify(baseInterceptor, times(1)).isLegalStr(anyString(), anyString(), anyBoolean());
        verify(baseInterceptor, times(1)).isIgnore(anyString());
        verify(baseInterceptor, times(1)).verifyToken(anyString());
        verify(baseInterceptor, times(1)).getLoginUrl();
    }
}