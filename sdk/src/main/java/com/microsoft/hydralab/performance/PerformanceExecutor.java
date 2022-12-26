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

    public void initDevice(PerfMetaInfo perfMetaInfo) {
        notifyEach(inspectors, recorder -> recorder.initDevice(perfMetaInfo));
    }

    public void addMetricsData(PerfMetaInfo perfMetaInfo) {
        notifyEach(inspectors, recorder -> recorder.addMetricsData(perfMetaInfo));
    }

    public List<PerfResult<?>> analyzeResult(PerfMetaInfo perfMetaInfo) {
        List<PerfResult<?>> perfResultList = new ArrayList<>();
        for (PerformanceInspector performanceInspector : inspectors) {
            perfResultList.add(performanceInspector.analyzeResult(perfMetaInfo));
        }
        return perfResultList;
    }
}
