// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.service;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.agent.config.AppOptions;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.agent.socket.AgentWebSocketClient;
import com.microsoft.hydralab.common.entity.common.AgentMetadata;
import com.microsoft.hydralab.common.entity.common.AgentUpdateTask;
import com.microsoft.hydralab.common.entity.common.AgentUser;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.Message;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.entity.common.TestTaskSpec;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.monitor.MetricPushGateway;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.GlobalConstant;
import com.microsoft.hydralab.common.util.ThreadUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.prometheus.client.exporter.BasicAuthHttpConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Service("WebSocketClient")
@Slf4j
public class AgentWebSocketClientService implements TestTaskRunCallback {
    @Value("${app.registry.name}")
    String agentName;
    @Value("${app.registry.id}")
    String agentId;
    @Value("${app.registry.secret}")
    String agentSecret;
    @Resource
    DeviceControlService deviceControlService;
    @Resource
    TestTaskEngineService testTaskEngineService;
    @Resource
    AgentManageService agentManageService;
    @Resource
    MeterRegistry meterRegistry;
    AgentUser agentUser;
    @Resource
    private StorageServiceClientProxy storageServiceClientProxy;
    private boolean isStorageClientInit = false;
    @Resource
    private AppOptions appOptions;
    @Resource
    private AgentWebSocketClient agentWebSocketClient;
    @Value("${agent.version}")
    private String versionName;
    @Value("${agent.versionCode}")
    private String versionCode;
    private SendMessageCallback sendMessageCallback;
    @Value("${management.metrics.export.prometheus.pushgateway.enabled}")
    private boolean isPrometheusEnabled;
    @Autowired(required = false)
    private MetricPushGateway pushGateway;
    boolean isAgentInit = false;

    public void onMessage(Message message) {
        log.info("onMessage Receive bytes message {}", message);
        String path = message.getPath();
        Message response = null;
        switch (path) {
            case Const.Path.AUTH:
                provideAuthInfo(message);
                return;
            case Const.Path.AGENT_INIT:
                if (!(message.getBody() instanceof AgentMetadata)) {
                    break;
                }
                heartbeatResponse(message);

                /** Sequence shouldn't be changed.
                 * [agentUser.setTeamName -> meterRegistry.config().commonTags -> deviceDriver.init
                 *  -> (deviceControlService.provideDeviceList + deviceStatbilityMonitor.addDeviceMetricRegistration)].
                 */
                if (!isAgentInit) {
                    registerAgentMetrics();
                    deviceControlService.deviceDriverInit();
                    isAgentInit = true;
                }
                deviceControlService.provideDeviceList(agentUser.getBatteryStrategy());
                return;
            case Const.Path.HEARTBEAT:
                if (!(message.getBody() instanceof AgentMetadata)) {
                    break;
                }
                heartbeatResponse(message);
                deviceControlService.provideDeviceList(agentUser.getBatteryStrategy());
                return;
            case Const.Path.DEVICE_UPDATE:
                if (!(message.getBody() instanceof JSONObject)) {
                    break;
                }
                JSONObject deviceData = (JSONObject) message.getBody();
                DeviceInfo device =
                        deviceControlService.updateDeviceScope(deviceData.getString(Const.AgentConfig.SERIAL_PARAM),
                                deviceData.getBoolean(Const.AgentConfig.SCOPE_PARAM));
                response = new Message();
                response.setPath(message.getPath());
                response.setSessionId(message.getSessionId());
                response.setBody(device);
                log.info("/api/device/update device SN: {}", device.getSerialNum());
                break;
            case Const.Path.AGENT_UPDATE:
                if (!(message.getBody() instanceof AgentUpdateTask)) {
                    break;
                }
                AgentUpdateTask taskInfo = (AgentUpdateTask) message.getBody();
                agentManageService.updateAgentPackage(taskInfo, path);
                break;
            case Const.Path.AGENT_RESTART:
                agentManageService.restartAgent(null, path);
                break;
            case Const.Path.DEVICE_LIST:
                if (agentUser.getBatteryStrategy() == null) {
                    response = new Message();
                    response.setPath(Const.Path.HEARTBEAT);
                    response.setSessionId(message.getSessionId());
                } else {
                    deviceControlService.provideDeviceList(agentUser.getBatteryStrategy());
                }
                break;
            case Const.Path.TEST_TASK_CANCEL:
                if (!(message.getBody() instanceof JSONObject)) {
                    break;
                }
                JSONObject data = (JSONObject) message.getBody();
                testTaskEngineService.cancelTestTaskById(data.getString(Const.AgentConfig.TASK_ID_PARAM));
                break;
            case Const.Path.TEST_TASK_RUN:
                try {
                    if (!(message.getBody() instanceof TestTaskSpec)) {
                        break;
                    }
                    TestTask testTask = testTaskEngineService.runTestTask((TestTaskSpec) message.getBody());
                    if (testTask == null) {
                        response = Message.error(message, 404, "No device meet the requirement");
                    } else {
                        response = Message.response(message, testTask);
                        response.setPath(Const.Path.TEST_TASK_UPDATE);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    response = Message.error(message, 500, e.getMessage() + e.getClass().getName());
                }
                break;
            default:
                break;
        }
        if (response == null) {
            return;
        }
        send(response);
    }

    private void heartbeatResponse(Message message) {
        AgentMetadata agentMetadata = (AgentMetadata) message.getBody();

        if (!isStorageClientInit) {
            storageServiceClientProxy.initAgentStorageClient(agentMetadata.getStorageType());
            isStorageClientInit = true;
        }
        storageServiceClientProxy.updateAccessToken(agentMetadata.getAccessToken());
        syncAgentStatus(agentMetadata.getAgentUser());
        prometheusPushgatewayInit(agentMetadata);
    }

    private void syncAgentStatus(AgentUser passedAgent) {
        agentUser.setTeamId(passedAgent.getTeamId());
        agentUser.setTeamName(passedAgent.getTeamName());
        agentUser.setBatteryStrategy(passedAgent.getBatteryStrategy());
    }

    private void prometheusPushgatewayInit(AgentMetadata agentMetadata) {
        if (isPrometheusEnabled && !pushGateway.isBasicAuthSet.get()) {
            pushGateway.setConnectionFactory(new BasicAuthHttpConnectionFactory(agentMetadata.getPushgatewayUsername(), agentMetadata.getPushgatewayPassword()));
            ThreadUtils.safeSleep(1000);
            pushGateway.isBasicAuthSet.set(true);
            log.info("Pushgateway has set basic auth now, data can be pushed correctly.");
        }
    }

    private void provideAuthInfo(Message message) {
        Message responseAuth = new Message();
        responseAuth.setSessionId(message.getSessionId());

        if (agentUser == null) {
            agentUser = new AgentUser();
        }
        agentUser.setId(agentId);
        agentUser.setName(agentName);
        agentUser.setSecret(agentSecret);
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            agentUser.setHostname(localHost.getHostName());
            // localHost.getHostAddress()
            agentUser.setIp("127.0.0.1");
        } catch (UnknownHostException ignore) {
        }
        agentUser.setOs(System.getProperties().getProperty("os.name"));
        agentUser.setVersionName(versionName);
        agentUser.setVersionCode(versionCode);
        responseAuth.setBody(agentUser);
        responseAuth.setPath(message.getPath());
        send(responseAuth);
    }

    public void send(Message message) {
        Assert.notNull(sendMessageCallback, "no send callback set");
        sendMessageCallback.send(message);
    }

    public void setSendMessageCallback(SendMessageCallback sendMessageCallback) {
        this.sendMessageCallback = sendMessageCallback;
    }

    @Override
    public void onTaskStart(TestTask testTask) {

    }

    @Override
    public void onTaskComplete(TestTask testTask) {
        log.info("test task {} onAllComplete in webclient, send message", testTask.getId());
        send(Message.ok(Const.Path.TEST_TASK_UPDATE, testTask));
    }

    @Override
    public void onOneDeviceComplete(TestTask testTask, TestRunDevice testRunDevice, Logger logger, TestRun result) {

    }

    @Override
    public void onDeviceOffline(TestTask testTask) {
        log.info("test task {} re-queue, send message", testTask.getId());
        send(Message.ok(Const.Path.TEST_TASK_RETRY, testTask));
    }

    public void registerAgentMetrics() {
        meterRegistry.config()
                .commonTags("computerName", agentUser.getHostname(), "agentName", agentUser.getName(), "teamName",
                        agentUser.getTeamName());

        registerAgentDiskUsageRatio();
        registerAgentReconnectRetryTimes();
        registerAgentRunningTestTaskNum();
    }

    public void registerAgentDiskUsageRatio() {
        meterRegistry.gauge(GlobalConstant.PROMETHEUS_METRIC_DISK_USAGE_RATIO,
                Tags.empty().and("disk", appOptions.getLocation().substring(0, 2)),
                appOptions.getLocation(),
                this::getPCDiskUsageRatio);
        log.info("Metric of disk usage ratio has been registered.");
    }

    public void registerAgentReconnectRetryTimes() {
        meterRegistry.gauge(GlobalConstant.PROMETHEUS_METRIC_WEBSOCKET_RECONNECT_RETRY_TIMES,
                Tags.empty(),
                agentWebSocketClient,
                this::getReconnectRetryTimes);
        log.info("Metric of agent reconnect retry times has been registered.");
    }

    public void registerAgentRunningTestTaskNum() {
        meterRegistry.gauge(GlobalConstant.PROMETHEUS_METRIC_RUNNING_TEST_NUM,
                Tags.empty(),
                testTaskEngineService,
                this::runningTestTaskNum);
        log.info("Metric of agent running test task number has been registered.");
    }

    private double getPCDiskUsageRatio(String appLocation) {
        File[] roots = File.listRoots();
        double diskUsageRatio = 0;
        for (File root : roots) {
            if (!appLocation.contains(root.getPath())) {
                continue;
            }

            diskUsageRatio = 1 - (double) root.getFreeSpace() / root.getTotalSpace();
            break;
        }
        return diskUsageRatio;
    }

    private int getReconnectRetryTimes(AgentWebSocketClient agentWebSocketClient) {
        return agentWebSocketClient.getReconnectTime();
    }

    private int runningTestTaskNum(TestTaskEngineService testTaskEngineService) {
        return testTaskEngineService.getRunningTestTask().size();
    }

    public interface SendMessageCallback {
        void send(Message message);
    }
}
