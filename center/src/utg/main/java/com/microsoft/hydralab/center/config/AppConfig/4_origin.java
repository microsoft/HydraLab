import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusProperties;
import java.net.MalformedURLException;

public class AppConfigTest {

    @Test
    public void testPushGateway() throws MalformedURLException {
        // Arrange
        PrometheusProperties prometheusProperties = Mockito.mock(PrometheusProperties.class);

        // Act
        AppConfig appConfig = new AppConfig();
        appConfig.pushGateway(prometheusProperties);

        // Assert
        // Add your assertions here
    }
}