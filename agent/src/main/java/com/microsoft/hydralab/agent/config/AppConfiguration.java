// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.microsoft.hydralab.agent.runner.smart.SmartTestUtil;
import com.microsoft.hydralab.agent.service.AgentWebSocketClientService;
import com.microsoft.hydralab.agent.socket.AgentWebSocketClient;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.Message;
import com.microsoft.hydralab.common.management.AgentType;
import com.microsoft.hydralab.common.management.AppiumServerManager;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.management.DeviceStabilityMonitor;
import com.microsoft.hydralab.common.management.impl.AndroidDeviceManager;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.blob.BlobStorageClient;
import io.micrometer.core.instrument.MeterRegistry;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Resource;
import java.io.File;
import java.net.URI;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author : shbu
 * @since 3.0
 */
@Configuration
public class AppConfiguration {
    @Value("${app.registry.agent-type}")
    public int agentTypeValue;
    Logger logger = LoggerFactory.getLogger(getClass());
    @Resource
    private AppOptions appOptions;
    @Value("${spring.profiles.active:@null}")
    private String activeProfile;
    @Value("${app.registry.server}")
    private String registryServer;
    @Value("${app.device.state-change.count-threshold}")
    private int deviceStateChangeThreshold;
    @Value("${app.device.state-change.window-time}")
    private long deviceStateChangeWindowTime;
    @Value("${app.device.state-change.recovery-time}")
    private long deviceStateChangeRecoveryTime;
    @Value("${app.adb.host:}")
    private String adbServerHost;
    @Value("${app.appium.host:}")
    private String appiumServerHost;

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("microsoft.hydra_lab.logThread-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

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
    public AgentWebSocketClient agentWebSocketClient(AgentWebSocketClientService agentWebSocketClientService) throws Exception {
        String wsUrl = String.format("ws://%s/agent/connect", registryServer);
        logger.info("connect to {}", wsUrl);
        AgentWebSocketClient agentWebSocketClient = new AgentWebSocketClient(new URI(wsUrl), agentWebSocketClientService);
        agentWebSocketClient.connect();
        return agentWebSocketClient;
    }

    @Bean
    public DeviceManager initDeviceManager(BlobStorageClient deviceLabBlobClient, ADBOperateUtil adbOperateUtil, AppiumServerManager appiumServerManager) {

        AgentType agentType = AgentType.formAgentType(agentTypeValue);
        DeviceManager deviceManager = agentType.getManager();
        if (deviceManager instanceof AndroidDeviceManager) {
            ((AndroidDeviceManager) deviceManager).setADBOperateUtil(adbOperateUtil);
        }
        if (StringUtils.isNotBlank(adbServerHost)) {
            logger.info("Setting the adb server hostname to {}", adbServerHost);
            adbOperateUtil.setAdbServerHost(adbServerHost);
        }
        File testBaseDir = new File(appOptions.getTestCaseResultLocation());
        if (!testBaseDir.exists()) {
            if (!testBaseDir.mkdirs()) {
                throw new RuntimeException("adbDeviceManager dir.mkdirs() failed: " + testBaseDir);
            }
        }
        deviceManager.setTestBaseDir(testBaseDir);
        deviceManager.setTestBaseDirUrlMapping(AppOptions.TEST_CASE_RESULT_STORAGE_MAPPING_REL_PATH);
        File deviceLogBaseDir = new File(appOptions.getDeviceLogStorageLocation());
        if (!deviceLogBaseDir.exists()) {
            if (!deviceLogBaseDir.mkdirs()) {
                throw new RuntimeException("adbDeviceManager dir.mkdirs() failed: " + deviceLogBaseDir);
            }
        }
        deviceManager.setDeviceLogBaseDir(deviceLogBaseDir);
        deviceManager.setBlobStorageClient(deviceLabBlobClient);

        deviceManager.setScreenshotDir(getScreenshotDir());
        deviceManager.setDeviceFolderUrlPrefix(AppOptions.DEVICE_STORAGE_MAPPING_REL_PATH);
        deviceManager.setDeviceStoragePath(appOptions.getDeviceStorageLocation());

        if (StringUtils.isNotBlank(appiumServerHost)) {
            logger.info("Setting the appium server hostname to {}", appiumServerHost);
            appiumServerManager.setAppiumServerHost(appiumServerHost);
        }
        appiumServerManager.setWorkspacePath(appOptions.getLocation());

        deviceManager.setAppiumServerManager(appiumServerManager);
        return deviceManager;
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
    public BlobStorageClient blobStorageClient() {
        return new BlobStorageClient();
    }

    @Bean
    public SmartTestUtil smartTestUtil() {
        return new SmartTestUtil(appOptions.getLocation());
    }

    @Bean
    public DeviceStabilityMonitor deviceStabilityMonitor(AgentWebSocketClientService agentWebSocketClientService, DeviceManager deviceManager, MeterRegistry meterRegistry) {
        DeviceStabilityMonitor deviceStabilityMonitor = new DeviceStabilityMonitor();

        deviceStabilityMonitor.setDeviceStateChangeThreshold(deviceStateChangeThreshold);
        deviceStabilityMonitor.setDeviceStateChangeWindowTime(deviceStateChangeWindowTime);
        deviceStabilityMonitor.setDeviceStateChangeRecoveryTime(deviceStateChangeRecoveryTime);
        deviceStabilityMonitor.setDeviceManager(deviceManager);
        deviceStabilityMonitor.setMeterRegistry(meterRegistry);

        deviceStabilityMonitor.setMonitor(new DeviceManager.DeviceStatusMonitor() {
            @Override
            public void onDeviceInactive(DeviceInfo deviceInfo) {
                //send message to master to update device status
                JSONObject data = new JSONObject();
                data.put(Const.AgentConfig.serial_param, deviceInfo.getSerialNum());
                if (DeviceInfo.UNSTABLE.equals(deviceInfo.getStatus())) {
                    data.put(Const.AgentConfig.status_param, deviceInfo.getStatus());
                } else {
                    data.put(Const.AgentConfig.status_param, DeviceInfo.OFFLINE);
                }
                agentWebSocketClientService.send(Message.ok(Const.Path.DEVICE_STATUS, data));
            }

            @Override
            public void onDeviceConnected(DeviceInfo deviceInfo) {
                //send message to master to update device status
                JSONObject data = new JSONObject();
                data.put(Const.AgentConfig.serial_param, deviceInfo.getSerialNum());
                if (DeviceInfo.UNSTABLE.equals(deviceInfo.getStatus())) {
                    data.put(Const.AgentConfig.status_param, deviceInfo.getStatus());
                } else {
                    data.put(Const.AgentConfig.status_param, DeviceInfo.ONLINE);
                }
                agentWebSocketClientService.send(Message.ok(Const.Path.DEVICE_STATUS, data));
            }
        });

        return deviceStabilityMonitor;
    }
}
