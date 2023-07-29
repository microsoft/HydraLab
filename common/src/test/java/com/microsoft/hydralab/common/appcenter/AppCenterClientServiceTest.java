// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.appcenter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.microsoft.hydralab.common.appcenter.entity.Device;
import com.microsoft.hydralab.common.appcenter.entity.HandledErrorLog;
import com.microsoft.hydralab.common.appcenter.entity.LogContainer;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

public class AppCenterClientServiceTest {

    private final String appCenterToken;

    {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("env.properties"));
            appCenterToken = properties.getProperty("APP_CENTER_TOKEN");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void sendCrashLogTest() throws Exception {
        LogContainer logContainer = new LogContainer();
        Device device = new Device();
        device.setSdkName("appcenter.android");                       // Example: "Android SDK"
        device.setSdkVersion("4.4.4");                 // Example: "1.2.3"
        device.setModel("Mi9");                          // Example: "iPad2,3"
        device.setOemName("Xiaomi");                 // Example: "HTC"
        device.setOsName("Android");                // Example: "iOS"
        device.setOsVersion("11");          // Example: "9.3.0"
        device.setOsBuild("LMY47X");         // Example: "LMY47X"
        device.setOsApiLevel(30);                                 // Example: 15
        device.setLocale("en_US");                                // Example: "en_US"
        device.setTimeZoneOffset(480);                            // Example: 300 (5 hours offset from UTC)
        device.setScreenSize("640x480");                          // Example: "640x480"
        device.setAppVersion("1.0.0");         // Example: "1.0.0"
        device.setAppBuild("10000");                   // Example: "42"
        device.setAppNamespace("com.microsoft.hydralab.android.client");         // Example: "com.example.app"
        String userId = UUID.randomUUID().toString();
        AppCenterErrorLogHandler appCenterErrorLogHandler = new AppCenterErrorLogHandler(device, userId);
        HandledErrorLog handledErrorLog = appCenterErrorLogHandler.createErrorLog(
                Thread.currentThread(), new Exception("test exception"), System.currentTimeMillis(), true);

//        handledErrorLog.setType(HandledErrorLog.TYPE);
        handledErrorLog.setSid(UUID.randomUUID());

        logContainer.getLogs().add(handledErrorLog);
        AppCenterClientService appCenterClientService = new AppCenterClientService();

        System.out.println(appCenterToken);
        System.out.println(JSON.toJSONString(logContainer, SerializerFeature.PrettyFormat));

        appCenterClientService.sendCrashLog(appCenterToken,
                UUID.randomUUID().toString(), logContainer);

    }


}
