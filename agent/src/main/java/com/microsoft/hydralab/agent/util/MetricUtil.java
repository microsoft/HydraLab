// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.util;

import com.microsoft.hydralab.agent.service.TestTaskEngineService;
import com.microsoft.hydralab.common.management.DeviceStabilityMonitor;
import com.microsoft.hydralab.common.util.GlobalConstant;
import com.microsoft.hydralab.agent.config.AppOptions;
import com.microsoft.hydralab.agent.service.AgentWebSocketClientService;
import com.microsoft.hydralab.agent.socket.AgentWebSocketClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

@Data
@Slf4j
public class MetricUtil {
    MeterRegistry meterRegistry;
    AgentWebSocketClientService agentWebSocketClientService;

    public void init(String hostName, String agentName){
        meterRegistry.config().commonTags("computerName", hostName, "agentName", agentName);
    }

    public void registerAgentDiskUsageRatio(AppOptions appOptions) {
        meterRegistry.gauge(GlobalConstant.PROMETHEUS_METRIC_DISK_USAGE_RATIO,
                Tags.empty().and("disk", appOptions.getLocation().substring(0, 2)),
                appOptions.getLocation(),
                this::getPCDiskUsageRatio);
        log.info("Metric of disk usage ratio has been registered.");
    }

    public void registerAgentReconnectRetryTimes(AgentWebSocketClient agentWebSocketClient) {
        meterRegistry.gauge(GlobalConstant.PROMETHEUS_METRIC_WEBSOCKET_RECONNECT_RETRY_TIMES,
                Tags.empty(),
                agentWebSocketClient,
                this::getReconnectRetryTimes);
        log.info("Metric of agent reconnect retry times has been registered.");
    }

    public void registerAgentRunningTestTaskNum(TestTaskEngineService deviceStabilityMonitor) {
        meterRegistry.gauge(GlobalConstant.PROMETHEUS_METRIC_RUNNING_TEST_NUM,
                Tags.empty(),
                deviceStabilityMonitor,
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
}
