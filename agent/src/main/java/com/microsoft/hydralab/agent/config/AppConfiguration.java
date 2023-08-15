// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.microsoft.hydralab.agent.environment.EnvCapabilityDiscoveryService;
import com.microsoft.hydralab.agent.runner.smart.SmartTestUtil;
import com.microsoft.hydralab.agent.service.AgentWebSocketClientService;
import com.microsoft.hydralab.agent.socket.AgentWebSocketClient;
import com.microsoft.hydralab.common.exception.reporter.AppCenterReporter;
import com.microsoft.hydralab.common.exception.reporter.ExceptionReporterManager;
import com.microsoft.hydralab.common.exception.reporter.FileReporter;
import com.microsoft.hydralab.common.entity.agent.LLMProperties;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.listener.DeviceStatusListenerManager;
import com.microsoft.hydralab.common.management.listener.impl.DeviceStabilityMonitor;
import com.microsoft.hydralab.common.monitor.MetricPushGateway;
import com.microsoft.hydralab.common.util.Const;
import io.micrometer.core.instrument.MeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusProperties;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

/**
 * @author : shbu
 * @since 3.0
 */
@Configuration
public class AppConfiguration {
    Logger logger = LoggerFactory.getLogger(getClass());
    @Resource
    private AppOptions appOptions;
    @Value("${app.registry.server}")
    private String registryServer;
    @Value("${app.device.state-change.count-threshold}")
    private int deviceStateChangeThreshold;
    @Value("${app.device.state-change.window-time}")
    private long deviceStateChangeWindowTime;
    @Value("${app.device.state-change.recovery-time}")
    private long deviceStateChangeRecoveryTime;
    @Value("${app.pre-install.shutdown-if-fail:true}")
    private Boolean shutdownIfFail;

    @NotNull
    private File getScreenshotDir() {
        String screenshotStorageLocation = appOptions.getScreenshotStorageLocation();
        File dir = new File(screenshotStorageLocation);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("create dir fail: " + dir.getAbsolutePath());
            }
        }
        return dir;
    }

    @Bean
    public EnvCapabilityDiscoveryService envCapabilityDiscoveryService() throws IOException {
        EnvCapabilityDiscoveryService envCapabilityDiscoveryService = new EnvCapabilityDiscoveryService();
        envCapabilityDiscoveryService.setEnableScan(true);
        envCapabilityDiscoveryService.discover();
        return envCapabilityDiscoveryService;
    }

    @Bean
    public AgentManagementService agentManagementService(StorageServiceClientProxy storageServiceClientProxy,
                                                         DeviceStatusListenerManager deviceStatusListenerManager,
                                                         EnvCapabilityDiscoveryService envCapabilityDiscoveryService) {
        AgentManagementService agentManagementService = new AgentManagementService();
        File testBaseDir = new File(appOptions.getTestCaseResultLocation());
        if (!testBaseDir.exists()) {
            if (!testBaseDir.mkdirs()) {
                throw new RuntimeException("agentManager dir.mkdirs() failed: " + testBaseDir);
            }
        }
        agentManagementService.setTestBaseDir(testBaseDir);
        File preAppDir = new File(appOptions.getPreAppStorageLocation());
        if (!preAppDir.exists()) {
            if (!preAppDir.mkdirs()) {
                throw new RuntimeException("agentManager dir.mkdirs() failed: " + preAppDir);
            }
        }
        agentManagementService.setPreAppDir(preAppDir);
        agentManagementService.setPreInstallFailurePolicy(
                shutdownIfFail ? Const.PreInstallFailurePolicy.SHUTDOWN : Const.PreInstallFailurePolicy.IGNORE);
        agentManagementService.setDeviceStatusListenerManager(deviceStatusListenerManager);
        agentManagementService.setTestBaseDirUrlMapping(AppOptions.TEST_CASE_RESULT_STORAGE_MAPPING_REL_PATH);
        File deviceLogBaseDir = new File(appOptions.getDeviceLogStorageLocation());
        if (!deviceLogBaseDir.exists()) {
            if (!deviceLogBaseDir.mkdirs()) {
                throw new RuntimeException("agentManager dir.mkdirs() failed: " + deviceLogBaseDir);
            }
        }
        agentManagementService.setDeviceLogBaseDir(deviceLogBaseDir);
        agentManagementService.setStorageServiceClientProxy(storageServiceClientProxy);

        agentManagementService.setScreenshotDir(getScreenshotDir());
        agentManagementService.setDeviceFolderUrlPrefix(AppOptions.DEVICE_STORAGE_MAPPING_REL_PATH);
        agentManagementService.setDeviceStoragePath(appOptions.getDeviceStorageLocation());

        agentManagementService.setEnvInfo(envCapabilityDiscoveryService.getEnvInfo());
        agentManagementService.setRegistryServer(registryServer);

        return agentManagementService;
    }

    // TODO: refactor test runner to remove DependsOn
    @Bean
    @DependsOn({"espressoRunner", "appiumRunner", "appiumCrossRunner", "smartRunner", "adbMonkeyRunner", "appiumMonkeyRunner", "t2cRunner", "xctestRunner"})
    public AgentWebSocketClient agentWebSocketClient(AgentWebSocketClientService agentWebSocketClientService)
            throws Exception {
        String wsUrl = String.format("ws://%s/agent/connect", registryServer);
        logger.info("connect to {}", wsUrl);
        AgentWebSocketClient agentWebSocketClient =
                new AgentWebSocketClient(new URI(wsUrl), agentWebSocketClientService);
        agentWebSocketClient.connect();
        return agentWebSocketClient;
    }

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder().build();
    }

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
    public SmartTestUtil smartTestUtil() {
        return new SmartTestUtil(appOptions.getLocation());
    }

    @Bean
    public DeviceStabilityMonitor deviceStabilityMonitor(AgentManagementService agentManagementService,
                                                         MeterRegistry meterRegistry) {
        DeviceStabilityMonitor deviceStabilityMonitor = new DeviceStabilityMonitor();

        deviceStabilityMonitor.setDeviceStateChangeThreshold(deviceStateChangeThreshold);
        deviceStabilityMonitor.setDeviceStateChangeWindowTime(deviceStateChangeWindowTime);
        deviceStabilityMonitor.setDeviceStateChangeRecoveryTime(deviceStateChangeRecoveryTime);
        deviceStabilityMonitor.setAgentManagementService(agentManagementService);
        deviceStabilityMonitor.setMeterRegistry(meterRegistry);

        return deviceStabilityMonitor;
    }

    @Bean
    @ConditionalOnProperty(prefix = "management.metrics.export.prometheus.pushgateway", name = "enabled", havingValue = "true")
    public MetricPushGateway pushGateway(PrometheusProperties prometheusProperties) throws MalformedURLException {
        String baseUrl = prometheusProperties.getPushgateway().getBaseUrl();
        if (!baseUrl.startsWith("http")) {
            if (baseUrl.startsWith("127.0.0.1") || baseUrl.startsWith("localhost")) {
                baseUrl = "http://" + baseUrl;
            } else {
                baseUrl = "https://" + baseUrl;
            }
        }

        // Set ConnectionFactory with basic auth later with metadata from Center message.
        return new MetricPushGateway(new URL(baseUrl));
    }

    @Bean
    @ConditionalOnProperty(prefix = "management.metrics.export.prometheus.pushgateway", name = "enabled", havingValue = "true")
    public PrometheusPushGatewayManager monitorPrometheusPushGatewayManager(PushGateway pushGateway,
                                                                            PrometheusProperties prometheusProperties,
                                                                            CollectorRegistry registry) {
        PrometheusProperties.Pushgateway properties = prometheusProperties.getPushgateway();
        Duration pushRate = properties.getPushRate();
        String job = properties.getJob();
        Map<String, String> groupingKey = properties.getGroupingKey();
        PrometheusPushGatewayManager.ShutdownOperation shutdownOperation = properties.getShutdownOperation();

        return new PrometheusPushGatewayManager(pushGateway, registry,
                pushRate, job, groupingKey, shutdownOperation);
    }

    @Bean
    public StorageServiceClientProxy storageServiceClientProxy(ApplicationContext applicationContext) {
        return new StorageServiceClientProxy(applicationContext);
    }

    @Bean
    public FileReporter fileReporter() {
        FileReporter fileReporter = new FileReporter(appOptions.getErrorStorageLocation());
        ExceptionReporterManager.registerExceptionReporter(fileReporter);
        return fileReporter;
    }

    @Bean
    public AppCenterReporter appCenterReporter() {
        AppCenterReporter appCenterReporter = new AppCenterReporter();
        return appCenterReporter;
    }

    @Bean
    @ConfigurationProperties(prefix = "runner.smart.llm")
    public LLMProperties llmProperties(){
        return new LLMProperties();
    }
}
