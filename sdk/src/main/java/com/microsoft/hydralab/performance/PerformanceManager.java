// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author zhoule
 * @date 12/14/2022
 */

public class PerformanceManager implements PerformanceInspector {
    List<PerformanceInspector> recorders = new ArrayList<>();

    private static <T extends PerformanceInspector> void notifyEach(List<T> recorders, Consumer<T> consumer) {
        recorders.forEach(recorder -> {
            consumer.accept(recorder);
        });
    }

    public void addRecorder(PerformanceInspector performanceInspector) {
        recorders.add(performanceInspector);
    }

    @Override
    public void initDevice() {
        notifyEach(recorders, recorder -> recorder.initDevice());
    }

    @Override
    public void addMetricsData(PerfMetaInfo perfMetaInfo) {
        notifyEach(recorders, recorder -> recorder.addMetricsData(perfMetaInfo));
    }

    @Override
    public void analyzeResult() {
        notifyEach(recorders, recorder -> recorder.analyzeResult());
    }
}
