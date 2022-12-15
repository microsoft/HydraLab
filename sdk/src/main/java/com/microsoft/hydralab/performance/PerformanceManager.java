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

public class PerformanceManager implements PerformanceRecorder {
    List<PerformanceRecorder> recorders = new ArrayList<>();

    private static <T extends PerformanceRecorder> void notifyEach(List<T> recorders, Consumer<T> consumer) {
        recorders.forEach(recorder -> {
            consumer.accept(recorder);
        });
    }

    public void addRecorder(PerformanceRecorder performanceRecorder) {
        recorders.add(performanceRecorder);
    }

    @Override
    public void beforeTest() {
        notifyEach(recorders, recorder -> recorder.beforeTest());
    }

    @Override
    public void addRecord() {
        notifyEach(recorders, recorder -> recorder.addRecord());
    }

    @Override
    public void afterTest() {
        notifyEach(recorders, recorder -> recorder.afterTest());
    }
}
