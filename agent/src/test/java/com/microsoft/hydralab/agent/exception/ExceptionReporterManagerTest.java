package com.microsoft.hydralab.agent.exception;

import com.microsoft.hydralab.agent.config.AppOptions;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.util.DateUtil;
import com.microsoft.hydralab.common.util.FileUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExceptionReporterManagerTest extends BaseTest {
    @Resource
    AppOptions appOptions;

    private static Object lock = new Object();
    final AtomicInteger count = new AtomicInteger(ROUNDS);
    static final int ROUNDS = 10;

    @Test
    void reportException() {
        count.set(ROUNDS);
        File folder = new File(appOptions.getErrorStorageLocation());
        if (folder.exists()) {
            FileUtil.deleteFile(folder);
        }
        folder.mkdir();
        Runnable logError = () -> {
            baseLogger.error("test exception: {}", Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            synchronized (lock) {
                if (count.decrementAndGet() == 0) {
                    lock.notifyAll();
                }
            }

        };
        for (int i = ROUNDS; i > 0; i--) {
            new Thread(logError).start();
        }
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        File errorFileFolder = new File(folder, DateUtil.ymdFormat.format(new Date()));
        Assertions.assertEquals(ROUNDS, errorFileFolder.listFiles().length, "should have " + ROUNDS + " file");
        FileUtil.deleteFile(folder);
    }

    @Test
    void reportWarning() {
        count.set(ROUNDS);
        File folder = new File(appOptions.getErrorStorageLocation());
        if (folder.exists()) {
            FileUtil.deleteFile(folder);
        }
        folder.mkdir();
        Runnable logWarn = () -> {
            baseLogger.warn("test warning: {}", Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            synchronized (lock) {
                if (count.decrementAndGet() == 0) {
                    lock.notifyAll();
                }
            }

        };
        for (int i = ROUNDS; i > 0; i--) {
            new Thread(logWarn).start();
        }
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        File warnFileFolder = new File(folder, DateUtil.ymdFormat.format(new Date()));
        Assertions.assertEquals(ROUNDS, warnFileFolder.listFiles().length, "should have " + ROUNDS + " file");
        FileUtil.deleteFile(folder);
    }
}