package com.microsoft.hydralab.performance;

import com.microsoft.hydralab.common.entity.common.TestTask;
import org.slf4j.Logger;

import java.util.List;

public class PerformanceTestNotifier {
    public void sendPerformanceNotification(List<PerformanceTestResult> resultList, TestTask testTask, Logger logger) {
        //TODO: Add more notification logic here. If the notify url in testTask is null or empty, we will not send notification.
    }

    public static class PerformanceNotification {
        public String pipelineLink;
        public String testTaskId;
        public List<PerformanceTestResult> resultList;
        public long testStartTimeMillis;
    }
}
