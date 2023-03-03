// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadPoolUtil {
    public static final ScheduledExecutorService TIMER_EXECUTOR = Executors.newScheduledThreadPool(20);
    public static final ScheduledExecutorService PERFORMANCE_TEST_TIMER_EXECUTOR = Executors.newScheduledThreadPool(5 /* corePoolSize */);
    private static final AtomicInteger SCREENSHOT_THREAD_NUMBER = new AtomicInteger(1);
    public static final Executor SCREENSHOT_EXECUTOR = newThreadPoolExecutor(SCREENSHOT_THREAD_NUMBER, 20, 60L, "ScreenshotExecutor");
    private static final AtomicInteger TEST_THREAD_NUMBER = new AtomicInteger(1);
    public static final Executor TEST_EXECUTOR = newThreadPoolExecutor(TEST_THREAD_NUMBER, 30, 60L, "TestExecutor");

    private ThreadPoolUtil() {

    }

    public static Executor newThreadPoolExecutor(AtomicInteger threadCounter, int corePoolSize, long keepAliveTimeSeconds, String threadNamePrefix) {
        return new ThreadPoolExecutor(corePoolSize, Integer.MAX_VALUE, keepAliveTimeSeconds,
                TimeUnit.SECONDS, new SynchronousQueue<>(),
                runnable -> {
                    Thread result = new Thread(runnable, threadNamePrefix + threadCounter.getAndIncrement());
                    result.setDaemon(false);
                    return result;
                });
    }
}
