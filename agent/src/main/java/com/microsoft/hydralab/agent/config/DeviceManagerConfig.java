// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.config;

import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.AppiumServerManager;
import com.microsoft.hydralab.common.management.device.impl.AndroidTestDeviceManager;
import com.microsoft.hydralab.common.management.device.impl.IOSTestDeviceManager;
import com.microsoft.hydralab.common.management.device.impl.WindowsTestDeviceManager;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhoule
 * @date 02/23/2023
 */
@Configuration
public class DeviceManagerConfig {

    Logger logger = LoggerFactory.getLogger(getClass());
    @Value("${app.adb.host:}")
    private String adbServerHost;
    @Value("${app.appium.host:}")
    private String appiumServerHost;

    @Bean
    public ADBOperateUtil adbOperateUtil() {
        ADBOperateUtil adbOperateUtil = new ADBOperateUtil();
        if (StringUtils.isNotBlank(adbServerHost)) {
            logger.info("Setting the adb server hostname to {}", adbServerHost);
            adbOperateUtil.setAdbServerHost(adbServerHost);
        }
        return adbOperateUtil;
    }

    @Bean
    public AppiumServerManager appiumServerManager() {
        AppiumServerManager appiumServerManager = new AppiumServerManager();
        if (StringUtils.isNotBlank(appiumServerHost)) {
            logger.info("Setting the appium server hostname to {}", appiumServerHost);
            appiumServerManager.setAppiumServerHost(appiumServerHost);
        }
        return appiumServerManager;
    }

    @Bean(name = "androidDeviceManager")
    public AndroidTestDeviceManager androidDeviceManager(AgentManagementService agentManagementService, AppiumServerManager appiumServerManager, ADBOperateUtil adbOperateUtil) {
        return new AndroidTestDeviceManager(agentManagementService, appiumServerManager, adbOperateUtil);
    }

    @Bean(name = "iosDeviceManager")
    public IOSTestDeviceManager iosDeviceManager(AgentManagementService agentManagementService, AppiumServerManager appiumServerManager) {
        return new IOSTestDeviceManager(agentManagementService, appiumServerManager);
    }

    @Bean(name = "windowsDeviceManager")
    public WindowsTestDeviceManager windowsDeviceManager(AgentManagementService agentManagementService, AppiumServerManager appiumServerManager) {
        return new WindowsTestDeviceManager(agentManagementService, appiumServerManager);
    }
}
