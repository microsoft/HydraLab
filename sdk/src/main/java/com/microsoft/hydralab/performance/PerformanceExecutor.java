// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PerformanceExecutor {
    List<PerformanceInspector> inspectors = new ArrayList<>();
    List<ScheduledFuture<?>> timerList = new ArrayList<>();
    File resultFolder;
    static final ScheduledExecutorService timerExecutor = Executors.newScheduledThreadPool(20 /* corePoolSize */);


    public PerformanceExecutor(File resultFolder) {
        this.resultFolder = resultFolder;
    }

    private static <T extends PerformanceInspector> void notifyEach(List<T> recorders, Consumer<T> consumer) {
        recorders.forEach(recorder -> {
            consumer.accept(recorder);
        });
    }

    public void addInspector(PerformanceInspector performanceInspector) {
        inspectors.add(performanceInspector);
    }

    public void initDevice(PerformanceTestSpec performanceTestSpec) {
        notifyEach(inspectors, recorder -> recorder.initialize(performanceTestSpec, resultFolder));
    }

    public void startCapturePerformanceTimer(PerformanceTestSpec performanceTestSpec, long interval) {
        ScheduledFuture<?> scheduledFuture = timerExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                capturePerformanceMetrics(performanceTestSpec);
            }
        }, interval, TimeUnit.SECONDS);
    }

    public void capturePerformanceMetrics(PerformanceTestSpec performanceTestSpec) {
        notifyEach(inspectors, recorder -> recorder.capturePerformanceMetrics(performanceTestSpec, resultFolder));
    }

    public List<PerformanceResult<?>> analyzeResult() {
        List<PerformanceResult<?>> performanceResultList = new ArrayList<>();
        for (PerformanceInspector performanceInspector : inspectors) {
            performanceResultList.add(performanceInspector.analyzeResults(resultFolder));
        }
        for (ScheduledFuture<?> timer : timerList) {
            timer.cancel(true);
        }
        return performanceResultList;
    }
}
