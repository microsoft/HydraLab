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
    List<ScheduledFuture<?>> capturePerformanceTimerList = new ArrayList<>();
    List<PerformanceInspectionResult> performanceInspectionResultList = new ArrayList<>();
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

    public void initialize(PerformanceTestSpec performanceTestSpec) {
        notifyEach(inspectors, recorder -> recorder.initialize(performanceTestSpec, resultFolder));
    }

    public void startInspectPerformanceTimer(PerformanceTestSpec performanceTestSpec, long interval) {
        initialize(performanceTestSpec);
        ScheduledFuture<?> scheduledFuture = timerExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                inspect(performanceTestSpec);
            }
        }, interval, TimeUnit.SECONDS);
    }

    public List<PerformanceInspectionResult> inspect(PerformanceTestSpec performanceTestSpec) {
        List<PerformanceInspectionResult> tempInspectionResultList = new ArrayList<>();
        for (PerformanceInspector performanceInspector : inspectors) {
            PerformanceInspectionResult result = performanceInspector.inspect(performanceTestSpec, resultFolder);
            if (result != null) {
                tempInspectionResultList.add(result);
            }
        }
        performanceInspectionResultList.addAll(tempInspectionResultList);
        return tempInspectionResultList;
    }

    public List<PerformanceResult<?>> parse() {
        List<PerformanceResult<?>> performanceResultList = new ArrayList<>();
        for (PerformanceInspector performanceInspector : inspectors) {
            performanceResultList.add(performanceInspector.parse(performanceInspectionResultList));
        }
        for (ScheduledFuture<?> timer : capturePerformanceTimerList) {
            timer.cancel(true);
        }
        return performanceResultList;
    }
}
