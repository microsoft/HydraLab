import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusProperties;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class AppConfigTest {

    @Test
    public void testMonitorPrometheusPushGatewayManager() throws MalformedURLException {
        PushGateway pushGateway = Mockito.mock(PushGateway.class);
        PrometheusProperties prometheusProperties = Mockito.mock(PrometheusProperties.class);
        CollectorRegistry registry = Mockito.mock(CollectorRegistry.class);

        PrometheusProperties.Pushgateway pushgatewayProperties = Mockito.mock(PrometheusProperties.Pushgateway.class);
        Mockito.when(prometheusProperties.getPushgateway()).thenReturn(pushgatewayProperties);
        Mockito.when(pushgatewayProperties.getBaseUrl()).thenReturn("http://localhost:9091");
        Mockito.when(pushgatewayProperties.getPushRate()).thenReturn(Duration.ofSeconds(10));
        Mockito.when(pushgatewayProperties.getJob()).thenReturn("testJob");

        Map<String, String> groupingKey = new HashMap<>();
        groupingKey.put("key1", "value1");
        groupingKey.put("key2", "value2");
        Mockito.when(pushgatewayProperties.getGroupingKey()).thenReturn(groupingKey);

        PrometheusPushGatewayManager.ShutdownOperation shutdownOperation = Mockito.mock(PrometheusPushGatewayManager.ShutdownOperation.class);
        Mockito.when(pushgatewayProperties.getShutdownOperation()).thenReturn(shutdownOperation);

        AppConfig appConfig = new AppConfig();
        appConfig.monitorPrometheusPushGatewayManager(pushGateway, prometheusProperties, registry);

        Mockito.verify(pushGateway, Mockito.times(1)).setConnectionFactory(Mockito.any());
        Mockito.verify(pushGateway, Mockito.times(1)).isBasicAuthSet();
        Mockito.verify(registry, Mockito.times(1)).register(Mockito.any());
    }
}