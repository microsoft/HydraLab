package com.microsoft.hydralab.common.exception.reporter;

import com.microsoft.hydralab.common.entity.common.AgentUser;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExceptionReporterManagerTest {

    @Test
    @Order(1)
    void registerExceptionReporter() {
        AgentUser agentUser = new AgentUser();
        AppCenterReporter appCenterReporter = new AppCenterReporter();
        appCenterReporter.initAppCenterReporter("appCenterSecret", agentUser.getName(), agentUser.getVersionName(), agentUser.getVersionCode());
        ExceptionReporterManager.registerExceptionReporter(appCenterReporter);
        ExceptionReporterManager.registerExceptionReporter(new FileReporter("errorOutput"));
    }

    @Test
    @Order(2)
    void reportException() {
        ExceptionReporterManager.reportException(new Exception("test exception1"));
    }

    @Test
    @Order(2)
    void testReportException() {
        ExceptionReporterManager.reportException(new Exception("test exception2"), Thread.currentThread());
    }
}