// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.appcenter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.microsoft.hydralab.common.appcenter.entity.HandledErrorLog;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.MockUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AppCenterClientServiceTest {

    private String appCenterToken;

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
    public void sendErrorLogTest() throws Exception {
        boolean mockHttpClient = false;
        if (StringUtils.isBlank(appCenterToken)) {
            appCenterToken = "test";
            // and we need to mock the http request
            mockHttpClient = true;
        }
        AppCenterClient appCenterClient = new AppCenterClient(appCenterToken, "agent");

        if (mockHttpClient) {
            appCenterClient.httpClient = MockUtil.mockOkHttpClient("{}");
        }

        HandledErrorLog handledErrorLog = appCenterClient.createErrorLog(Thread.currentThread(), new HydraLabRuntimeException("sendErrorLogTest exception"), true);

        System.out.println(JSON.toJSONString(handledErrorLog, SerializerFeature.PrettyFormat));

        appCenterClient.send(handledErrorLog);

    }


}
