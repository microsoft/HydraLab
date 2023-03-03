// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.config;

import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.center.util.MetricUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.Resource;
import javax.servlet.ServletContext;

@Order(2)
@Component
@Profile({"default", "release", "dev"})
@Slf4j
public class ContextStartup implements ApplicationRunner, ServletContextAware {

    @Resource
    MetricUtil metricUtil;
    @Resource
    DeviceAgentManagementService deviceAgentManagementService;
    private ServletContext servletContext;

    @Override
    public void run(ApplicationArguments applicationArguments) {
        initMetricCollect(deviceAgentManagementService);
        log.info("initialization ...");
        log.info("OK, completed");
    }

    private void initMetricCollect(DeviceAgentManagementService deviceAgentManagementService) {
        metricUtil.registerOnlineAgent(deviceAgentManagementService);
        metricUtil.registerOnlineDevice(deviceAgentManagementService);
    }

    @Override
    public void setServletContext(@NotNull ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
