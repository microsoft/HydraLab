import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
 MockitoAnnotations.initMocks(this); baseInterceptor = new BaseInterceptor(); 
}

@Test
public void testAfterConcurrentHandlingStarted() {
 baseInterceptor.afterConcurrentHandlingStarted(request, response, handler); 
}

}
