package com.microsoft.hydralab.performance;

import org.slf4j.Logger;

public class TestNotifier {
    public void sendTestNotification(String notifyURL, TestNotification notification, Logger logger) {
        //TODO: Add more notification logic here. If the notify url in testTask is null or empty, we will not send notification.
    }

    public static class TestNotification {
        public String pipelineLink;
        public String testTaskId;
        // The content of the notification. It can be a list of performance results.
        public Object content;
        public long testStartTimeMillis;
    }
}
