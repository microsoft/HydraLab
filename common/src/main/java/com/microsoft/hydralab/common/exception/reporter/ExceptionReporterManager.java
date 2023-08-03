// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.exception.reporter;

import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author zhoule
 * @date 08/01/2023
 */

public class ExceptionReporterManager {
    static List<ExceptionReporter> reporters = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(ExceptionReporterManager.class);

    private static <T extends ExceptionReporter> void notifyEach(List<T> recorders, Consumer<T> consumer) {
        recorders.forEach(recorder -> {
            try {
                consumer.accept(recorder);
            } catch (HydraLabRuntimeException e) {
                logger.warn("Failed to notify recorder: " + recorder.getClass().getName(), e);
            }
        });
    }

    public static void registerExceptionReporter(@NotNull ExceptionReporter reporter) {
        reporters.add(reporter);
    }

    public static void reportException(Exception e) {
        notifyEach(reporters, reporter -> reporter.reportException(e));
    }

    public static void reportException(Exception e, Thread thread) {
        notifyEach(reporters, reporter -> reporter.reportException(e, thread));
    }
}
