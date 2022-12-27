// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PerformanceExecutor {
    List<PerformanceInspector> inspectors = new ArrayList<>();

    private static <T extends PerformanceInspector> void notifyEach(List<T> recorders, Consumer<T> consumer) {
        recorders.forEach(recorder -> {
            consumer.accept(recorder);
        });
    }

    public void addInspector(PerformanceInspector performanceInspector) {
        inspectors.add(performanceInspector);
    }

    public void initDevice(PerformanceTestSpec performanceTestSpec) {
        notifyEach(inspectors, recorder -> recorder.initialize(performanceTestSpec));
    }

    public void capturePerformanceMetrics(PerformanceTestSpec performanceTestSpec) {
        notifyEach(inspectors, recorder -> recorder.capturePerformanceMetrics(performanceTestSpec));
    }

    public List<PerformanceResult<?>> analyzeResult(PerformanceTestSpec performanceTestSpec) {
        List<PerformanceResult<?>> performanceResultList = new ArrayList<>();
        for (PerformanceInspector performanceInspector : inspectors) {
            performanceResultList.add(performanceInspector.analyzeResult(performanceTestSpec));
        }
        return performanceResultList;
    }
}
