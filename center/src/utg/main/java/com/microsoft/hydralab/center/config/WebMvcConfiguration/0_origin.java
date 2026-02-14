import com.microsoft.hydralab.center.config.WebMvcConfiguration;
import com.microsoft.hydralab.center.interceptor.BaseInterceptor;
import com.microsoft.hydralab.center.interceptor.CorsInterceptor;
import com.microsoft.hydralab.common.util.Const;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;

import static org.mockito.Mockito.*;

public class WebMvcConfigurationTest {

    @Mock
    private BaseInterceptor baseInterceptor;
    @Mock
    private CorsInterceptor corsInterceptor;
    @Mock
    private FastJsonHttpMessageConverter fastJsonHttpMessageConverter;
    @Mock
    private ViewControllerRegistry viewControllerRegistry;
    @Mock
    private InterceptorRegistry interceptorRegistry;

    private WebMvcConfiguration webMvcConfiguration;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        webMvcConfiguration = new WebMvcConfiguration();
    }

    @Test
    public void testAddViewControllers() {
        webMvcConfiguration.addViewControllers(viewControllerRegistry);
        verify(viewControllerRegistry).addViewController("/portal/").setViewName("forward:" + Const.FrontEndPath.INDEX_PATH);
    }

    @Test
    public void testAddInterceptors() {
        webMvcConfiguration.addInterceptors(interceptorRegistry);
        verify(interceptorRegistry).addInterceptor(baseInterceptor);
        verify(baseInterceptor).addPathPatterns(anyString());
    }
}