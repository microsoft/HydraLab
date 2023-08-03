package com.microsoft.hydralab.agent.exception;

import com.microsoft.hydralab.agent.config.AppOptions;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.exception.reporter.AppCenterReporter;
import com.microsoft.hydralab.common.exception.reporter.ExceptionReporterManager;
import com.microsoft.hydralab.common.exception.reporter.FileReporter;
import com.microsoft.hydralab.common.util.FileUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.annotation.Resource;
import java.io.File;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExceptionReporterManagerTest extends BaseTest {
    @Resource
    AppOptions appOptions;

    @Test
    @Order(1)
    void registerExceptionReporter() {
        AppCenterReporter appCenterReporter = new AppCenterReporter();
        ExceptionReporterManager.registerExceptionReporter(appCenterReporter);
        ExceptionReporterManager.registerExceptionReporter(new FileReporter(appOptions.getErrorStorageLocation()));
    }

    @Test
    @Order(2)
    void reportException() {
        File folder = new File(appOptions.getErrorStorageLocation());
        if (folder.exists()) {
            FileUtil.deleteFile(folder);
        }
        folder.mkdir();
        ExceptionReporterManager.reportException(new Exception("test exception1"));
        Assertions.assertEquals(1, folder.listFiles().length, "should have one file");
    }

    @Test
    @Order(2)
    void testReportException() {
        File folder = new File(appOptions.getErrorStorageLocation());
        if (folder.exists()) {
            FileUtil.deleteFile(folder);
        }
        folder.mkdir();
        ExceptionReporterManager.reportException(new Exception("test exception2"), Thread.currentThread());
        Assertions.assertEquals(1, folder.listFiles().length, "should have one file");
    }
}