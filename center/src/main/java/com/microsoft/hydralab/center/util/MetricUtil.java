// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.center.config.RestTemplateConfig;
import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.common.entity.center.AgentUser;
import com.microsoft.hydralab.common.util.GlobalConstant;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;

@Service
@Slf4j
public class MetricUtil {
    @Resource
    MeterRegistry meterRegistry;
    //save agent status <AgentUser ID, live status>
    private final HashMap<String, String> agentAliveStatusMap = new HashMap<>();

    public void registerOnlineAgent(DeviceAgentManagementService deviceAgentManagementService) {
        meterRegistry.gauge(GlobalConstant.PROMETHEUS_METRIC_ONLINE_AGENT_NUM,
                Tags.empty(),
                deviceAgentManagementService,
                this::getOnlineAgentNum);
        log.info("Metric of agent online number has been registered.");
    }

    public void registerOnlineDevice(DeviceAgentManagementService deviceAgentManagementService) {
        meterRegistry.gauge(GlobalConstant.PROMETHEUS_METRIC_ONLINE_DEVICE_NUM,
                Tags.empty(),
                deviceAgentManagementService,
                this::getAliveDeviceNum);
        log.info("Metric of device online number has been registered.");
    }

    public void registerAgentAliveStatusMetric(AgentUser agentUser) {
        if (agentAliveStatusMap.containsKey(agentUser.getId())) {
            updateAgentAliveStatus(agentUser.getId(), GlobalConstant.AgentLiveStatus.ONLINE.getStatus());
            return;
        }
        updateAgentAliveStatus(agentUser.getId(), GlobalConstant.AgentLiveStatus.ONLINE.getStatus());

        meterRegistry.gauge(GlobalConstant.PROMETHEUS_METRIC_WEBSOCKET_DISCONNECT_SIGNAL,
                Tags.empty().and("computerName", agentUser.getHostname(), "agentName", agentUser.getName(), "teamName", agentUser.getTeamName()),
                agentUser.getId(),
                this::getAgentAliveStatus);
        log.info("Status metric of agent {} has been registered.", agentUser.getName());
    }

    public void updateAgentAliveStatus(String agentId, String status) {
        agentAliveStatusMap.put(agentId, status);
    }


    public int getAgentAliveStatus(String agentUserId) {
        String agentStatus = agentAliveStatusMap.getOrDefault(agentUserId, GlobalConstant.AgentLiveStatus.OFFLINE.getStatus());
        return GlobalConstant.AgentLiveStatus.OFFLINE.getStatus().equals(agentStatus) ? 1 : 0;
    }

    public int getOnlineAgentNum(DeviceAgentManagementService deviceAgentManagementService) {
        return deviceAgentManagementService.getAgentNum();
    }

    public int getAliveDeviceNum(DeviceAgentManagementService deviceAgentManagementService) {
        return deviceAgentManagementService.getAliveDeviceNum();
    }

    // todo: copy agent/device data from Grafana into sql server
    @Scheduled(cron = "0 0 1 * * *")
    public int getGrafanaOnlineAgentNumber() {
        int agentNum = 0;
        try {
            RestTemplate restTemplateHttps = new RestTemplate(RestTemplateConfig.generateHttpRequestFactory());
            restTemplateHttps.getMessageConverters().add(new StringHttpMessageConverter());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String json = "{\"queries\":[{\"datasource\":{\"uid\":\"h3Lpj_6nk\",\"type\":\"prometheus\"},\"editorMode\":\"builder\",\"expr\":\"count(agent_ws_disconnect_signal == 0)\",\"legendFormat\":\"__auto\",\"range\":true,\"refId\":\"A\",\"queryType\":\"timeSeriesQuery\",\"exemplar\":false,\"requestId\":\"12A\",\"utcOffsetSec\":28800,\"interval\":\"\",\"datasourceId\":1,\"intervalMs\":300000,\"maxDataPoints\":1482}],\"range\":{\"from\":\"2022-11-01T10:21:07.980Z\",\"to\":\"2022-11-08T10:21:07.980Z\",\"raw\":{\"from\":\"now-7d\",\"to\":\"now\"}},\"from\":\"1667298067980\",\"to\":\"1667902867980\"}";
            HttpEntity<String> entity = new HttpEntity<>(json, headers);

            String testUrl = "https://hydradevicenetwork.azurewebsites.net/grafana/api/ds/query";
            ResponseEntity<JSONObject> response = restTemplateHttps.exchange(testUrl, HttpMethod.POST, entity, JSONObject.class);
            JSONArray dataArray = response.getBody().getJSONObject("results")
                    .getJSONObject("A").getJSONArray("frames").getJSONObject(0)
                    .getJSONObject("data").getJSONArray("values").getJSONArray(1);
            // todo : parse agentNum
//            agentNum
        } catch (Exception e) {
            e.printStackTrace();
        }

        return agentNum;
    }


}
