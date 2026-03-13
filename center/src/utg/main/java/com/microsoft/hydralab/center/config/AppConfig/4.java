import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusProperties;
import java.net.MalformedURLException;

public class AppConfigTest {
@Test
public void testPushGateway() {
 PrometheusProperties prometheusProperties = Mockito.mock(PrometheusProperties.class); AppConfig appConfig = new AppConfig(); appConfig.pushGateway(prometheusProperties); 
}

}
