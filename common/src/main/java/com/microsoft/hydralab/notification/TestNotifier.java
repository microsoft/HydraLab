// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.notification;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.util.RestTemplateConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

public class TestNotifier {

    public void sendTestNotification(String notifyURL, TestNotification notification, @NotNull Logger logger) {
        if (StringUtils.isEmpty(notifyURL)) {
            logger.info("The notify url is empty, we will not send notification.");
            return;
        }

        logger.info("Send notification with {} to {}", notification, notifyURL);
        try {
            RestTemplate restTemplateHttps = new RestTemplate(RestTemplateConfig.generateHttpRequestFactory());
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/json; charset=UTF-8");
            HttpEntity<String> entity = new HttpEntity<>(JSONObject.toJSONString(notification), headers);
            ResponseEntity<JSONObject> response = restTemplateHttps.exchange(notifyURL, HttpMethod.POST, entity, JSONObject.class);
            logger.info("Send notification with response {}", response);
        } catch (Exception e) {
            logger.error("Failed to send notification with {} to {}", notification, notifyURL, e);
        }
    }

    public static class TestNotification {
        public String reportLink;
        public String testTaskId;
        // The content of the notification. It can be a list of performance results.
        public Object content;
        public String testStartTime;
    }
}
