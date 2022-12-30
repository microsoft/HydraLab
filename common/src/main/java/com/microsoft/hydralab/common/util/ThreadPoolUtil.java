// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolUtil {
    private static final AtomicInteger threadNumber = new AtomicInteger(1);
    public static final Executor executor = newThreadPoolExecutor(threadNumber, 20, 60L, "");

    public static Executor newThreadPoolExecutor(AtomicInteger threadCounter, int corePoolSize, long keepAliveTimeSeconds, String threadNamePrefix) {
        Thread thread = new Thread();
        thread.interrupt();
        return new ThreadPoolExecutor(corePoolSize /* corePoolSize */,
                Integer.MAX_VALUE /* maximumPoolSize */, keepAliveTimeSeconds /* keepAliveTime */, TimeUnit.SECONDS,
                new SynchronousQueue<>(), runnable -> {
            Thread result = new Thread(runnable, threadNamePrefix + threadCounter.getAndIncrement());
            result.setDaemon(false);
            return result;
        });
    }
}
