// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.config;

import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.AppiumServerManager;
import com.microsoft.hydralab.common.management.device.DeviceType;
import com.microsoft.hydralab.common.management.device.impl.AndroidDeviceDriver;
import com.microsoft.hydralab.common.management.device.impl.DeviceDriverManager;
import com.microsoft.hydralab.common.management.device.impl.IOSDeviceDriver;
import com.microsoft.hydralab.common.management.device.impl.WindowsDeviceDriver;
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
public class DeviceDriverConfig {

    @Value("${app.device.monitor.android.enabled:false}")
    boolean enableAndroid;
    @Value("${app.device.monitor.windows.enabled:false}")
    boolean enableWindows;
    @Value("${app.device.monitor.ios.enabled:false}")
    boolean enableIos;
    Logger logger = LoggerFactory.getLogger(getClass());
    @Value("${app.adb.host:}")
    private String adbServerHost;
    @Value("${app.appium.host:}")
    private String appiumServerHost;

    @Bean
    public DeviceDriverManager deviceDriverManager(AgentManagementService agentManagementService, AppiumServerManager appiumServerManager, ADBOperateUtil adbOperateUtil) {
        DeviceDriverManager deviceDriverManager = new DeviceDriverManager();
        if (enableAndroid) {
            AndroidDeviceDriver androidDeviceDriver = new AndroidDeviceDriver(agentManagementService, appiumServerManager, adbOperateUtil);
            deviceDriverManager.addDeviceDriver(DeviceType.ANDROID, androidDeviceDriver);
        }
        if (enableIos) {
            IOSDeviceDriver iosDeviceDriver = new IOSDeviceDriver(agentManagementService, appiumServerManager);
            deviceDriverManager.addDeviceDriver(DeviceType.IOS, iosDeviceDriver);
        }
        if (enableWindows) {
            WindowsDeviceDriver windowsDeviceDriver = new WindowsDeviceDriver(agentManagementService, appiumServerManager);
            deviceDriverManager.addDeviceDriver(DeviceType.WINDOWS, windowsDeviceDriver);
        }
        return deviceDriverManager;
    }

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
}
