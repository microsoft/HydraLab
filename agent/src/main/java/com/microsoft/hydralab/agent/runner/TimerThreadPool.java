// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhoule
 * @date 12/30/2022
 */
public class TimerThreadPool {
    private static final AtomicInteger threadNumber = new AtomicInteger(1);
    public static final Executor executor = new ThreadPoolExecutor(20,
            Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            runnable -> {
                Thread result = new Thread(runnable, "TimerThreadPool-" + threadNumber.getAndIncrement());
                result.setDaemon(false);
                return result;
            });
}