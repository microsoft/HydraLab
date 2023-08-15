import com.microsoft.hydralab.center.interceptor.CorsInterceptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static org.mockito.Mockito.*;

public class CorsInterceptorTest {
@Mock
private HttpServletRequest request;
@Mock
private HttpServletResponse response;
@Mock
private Object handler;
@Mock
private Exception ex;
private CorsInterceptor corsInterceptor;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); corsInterceptor = new CorsInterceptor(); 
}

@Test
public void testAfterCompletion() {
 corsInterceptor.afterCompletion(request, response, handler, ex); 
}

}
