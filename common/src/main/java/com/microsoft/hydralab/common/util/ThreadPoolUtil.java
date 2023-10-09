// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.util;

import com.microsoft.hydralab.common.exception.handler.ThreadExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class ThreadPoolUtil {

    public static final ScheduledExecutorService TIMER_EXECUTOR =
            Executors.newScheduledThreadPool(20, new HydraThreadFactory(new ThreadExceptionHandler(), "CommonTimer"));
    public static final ScheduledExecutorService PERFORMANCE_TEST_TIMER_EXECUTOR =
            Executors.newScheduledThreadPool(5 /* corePoolSize */, new HydraThreadFactory(new ThreadExceptionHandler(), "PerformanceTestTimer"));
    public static final Executor SCREENSHOT_EXECUTOR =
            newThreadPoolExecutor(20, 60L, "ScreenshotExecutor");
    public static final Executor TEST_EXECUTOR =
            newThreadPoolExecutor(30, 60L, "TestExecutor");

    public static Executor newThreadPoolExecutor(int corePoolSize, long keepAliveTimeSeconds, String threadNamePrefix) {
        return new ThreadPoolExecutor(corePoolSize, Integer.MAX_VALUE, keepAliveTimeSeconds,
                TimeUnit.SECONDS, new SynchronousQueue<>(),
                new HydraThreadFactory(new ThreadExceptionHandler(), threadNamePrefix));
    }

    public static class HydraThreadFactory implements ThreadFactory {
        private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
        private final AtomicInteger threadCounter = new AtomicInteger(1);
        private final String threadNamePrefix;

        public HydraThreadFactory(Thread.UncaughtExceptionHandler uncaughtExceptionHandler, String threadNamePrefix) {
            this.uncaughtExceptionHandler = uncaughtExceptionHandler;
            this.threadNamePrefix = threadNamePrefix;
        }

        @Override
        public Thread newThread(@NotNull Runnable run) {
            Thread thread = new Thread(run, threadNamePrefix + threadCounter.getAndIncrement());
            thread.setDaemon(false);
            thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
            return thread;
        }
    }
}
