import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.Mockito.verify;

public class WebMvcConfigurationTest {

    @Mock
    private BaseInterceptor baseInterceptor;

    @Mock
    private CorsInterceptor corsInterceptor;

    @Mock
    private InterceptorRegistry registry;

    private WebMvcConfiguration webMvcConfiguration;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        webMvcConfiguration = new WebMvcConfiguration();
    }

    @Test
    public void testAddInterceptors() {
        webMvcConfiguration.addInterceptors(registry);

        verify(registry).addInterceptor(baseInterceptor);
    }
}