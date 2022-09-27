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
    public static final Executor executor = new ThreadPoolExecutor(20 /* corePoolSize */,
            Integer.MAX_VALUE /* maximumPoolSize */, 60L /* keepAliveTime */, TimeUnit.SECONDS,
            new SynchronousQueue<>(), runnable -> {
        Thread result = new Thread(runnable, "ThreadPoolUtil-" + threadNumber.getAndIncrement());
        result.setDaemon(false);
        return result;
    });
}
