import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
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
@Mock
private ModelAndView modelAndView;
private BaseInterceptor baseInterceptor;
@Before
public void setUp() {
 MockitoAnnotations.initMocks(this); baseInterceptor = new BaseInterceptor(); 
}

@Test
public void testPostHandle() {
 baseInterceptor.postHandle(request, response, handler, modelAndView); 
}

}
