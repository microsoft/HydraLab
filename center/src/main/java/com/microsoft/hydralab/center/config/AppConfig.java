// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.microsoft.hydralab.common.monitor.MetricPushGateway;
import com.microsoft.hydralab.common.entity.center.BlobProperty;
import com.microsoft.hydralab.common.util.blob.BlobStorageClient;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.BasicAuthHttpConnectionFactory;
import io.prometheus.client.exporter.PushGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusProperties;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

@Configuration
@ComponentScan(basePackages = {"com.microsoft.hydralab"})
public class AppConfig {
    @Value("${management.metrics.export.prometheus.pushgateway.username}")
    private String pushgatewayUsername;
    @Value("${management.metrics.export.prometheus.pushgateway.password}")
    private String pushgatewayPassword;

    @Bean
    @ConditionalOnClass({JSON.class})
    public FastJsonHttpMessageConverter fastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter fastConverter = new FastJsonHttpMessageConverter();

        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setSerializerFeatures(
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullStringAsEmpty,
                SerializerFeature.WriteNullListAsEmpty,
                SerializerFeature.DisableCircularReferenceDetect
        );
        fastConverter.setFastJsonConfig(fastJsonConfig);
        return fastConverter;
    }

    @Bean
    public BlobStorageClient blobStorageClient(BlobProperty blobProperty) {
        return new BlobStorageClient(blobProperty);
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher eventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public PushGateway pushGateway(PrometheusProperties prometheusProperties) throws MalformedURLException {
        String baseUrl = prometheusProperties.getPushgateway().getBaseUrl();
        if (!baseUrl.startsWith("http")){
            if (baseUrl.startsWith("127.0.0.1") || baseUrl.startsWith("localhost")) {
                baseUrl = "http://" + baseUrl;
            }
            else {
                baseUrl = "https://" + baseUrl;
            }
        }
        MetricPushGateway pushGateway = new MetricPushGateway(new URL(baseUrl));
        pushGateway.setConnectionFactory(new BasicAuthHttpConnectionFactory(pushgatewayUsername, pushgatewayPassword));
        pushGateway.isBasicAuthSet.set(true);

        return pushGateway;
    }

    @Bean
    public PrometheusPushGatewayManager monitorPrometheusPushGatewayManager(PushGateway pushGateway, PrometheusProperties prometheusProperties, CollectorRegistry registry) {
        PrometheusProperties.Pushgateway properties = prometheusProperties.getPushgateway();
        Duration pushRate = properties.getPushRate();
        String job = properties.getJob();
        Map<String, String> groupingKey = properties.getGroupingKey();
        PrometheusPushGatewayManager.ShutdownOperation shutdownOperation = properties.getShutdownOperation();

        return new PrometheusPushGatewayManager(pushGateway, registry,
                pushRate, job, groupingKey, shutdownOperation);
    }
}
