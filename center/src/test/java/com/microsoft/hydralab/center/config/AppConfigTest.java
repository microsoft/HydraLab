package com.microsoft.hydralab.center.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.mockito.Mockito;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusProperties;
import java.net.MalformedURLException;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusProperties;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class AppConfigTest {

    @Mock
    private FastJsonConfig fastJsonConfig;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private SessionRegistry sessionRegistry;
    @Mock
    private HttpSessionEventPublisher httpSessionEventPublisher;
    @Mock
    private FileReporter fileReporter;
    @Mock
    private AppCenterReporter appCenterReporter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testFastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter fastConverter = new FastJsonHttpMessageConverter();
        List<MediaType> supportedMediaTypes = new ArrayList<>();
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);
        supportedMediaTypes.add(MediaType.APPLICATION_ATOM_XML);
        supportedMediaTypes.add(MediaType.APPLICATION_FORM_URLENCODED);
        supportedMediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
        supportedMediaTypes.add(MediaType.APPLICATION_PDF);
        supportedMediaTypes.add(MediaType.APPLICATION_RSS_XML);
        supportedMediaTypes.add(MediaType.APPLICATION_XHTML_XML);
        supportedMediaTypes.add(MediaType.APPLICATION_XML);
        supportedMediaTypes.add(MediaType.IMAGE_GIF);
        supportedMediaTypes.add(MediaType.IMAGE_JPEG);
        supportedMediaTypes.add(MediaType.IMAGE_PNG);
        supportedMediaTypes.add(MediaType.TEXT_EVENT_STREAM);
        supportedMediaTypes.add(MediaType.TEXT_HTML);
        supportedMediaTypes.add(MediaType.TEXT_MARKDOWN);
        supportedMediaTypes.add(MediaType.TEXT_PLAIN);
        supportedMediaTypes.add(MediaType.TEXT_XML);
        fastConverter.setSupportedMediaTypes(supportedMediaTypes);
        when(fastJsonConfig.getSerializerFeatures()).thenReturn(
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullStringAsEmpty,
                SerializerFeature.WriteNullListAsEmpty,
                SerializerFeature.DisableCircularReferenceDetect
        );
        fastConverter.setFastJsonConfig(fastJsonConfig);
        assertEquals(supportedMediaTypes, fastConverter.getSupportedMediaTypes());
        assertEquals(fastJsonConfig, fastConverter.getFastJsonConfig());
    }

    @Test
    public void testStorageServiceClientProxy() {
        AppConfig appConfig = new AppConfig();
        appConfig.storageServiceClientProxy(applicationContext);
    }

    @Test
    public void testSessionRegistry() {
        SessionRegistryImpl sessionRegistryImpl = new SessionRegistryImpl();
    }

    @Test
    public void testEventPublisher() {
        AppConfig appConfig = new AppConfig();
        HttpSessionEventPublisher eventPublisher = appConfig.eventPublisher();
    }

    @Test
    public void testPushGateway() {
        PrometheusProperties prometheusProperties = Mockito.mock(PrometheusProperties.class);
        AppConfig appConfig = new AppConfig();
        appConfig.pushGateway(prometheusProperties);
    }

    @Test
    public void testMonitorPrometheusPushGatewayManager() {
        PushGateway pushGateway = Mockito.mock(PushGateway.class);
        PrometheusProperties prometheusProperties = Mockito.mock(PrometheusProperties.class);
        CollectorRegistry registry = Mockito.mock(CollectorRegistry.class);
        PrometheusProperties.Pushgateway pushgatewayProperties = Mockito.mock(PrometheusProperties.Pushgateway.class);
        Mockito.when(prometheusProperties.getPushgateway()).thenReturn(pushgatewayProperties);
        Mockito.when(pushgatewayProperties.getBaseUrl()).thenReturn("http:");
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

    @Test
    public void testFileReporter() {
        AppConfig appConfig = new AppConfig();
        appConfig.fileReporter();
        verify(fileReporter, times(1)).init(anyString());
        verify(fileReporter, times(1)).registerExceptionReporter();
    }

    @Test
    public void testAppCenterReporter() {

    }

}
