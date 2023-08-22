// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.exception.handler;

import com.microsoft.hydralab.common.exception.reporter.ExceptionReporterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhoule
 * @date 08/02/2023
 */

public class ThreadExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(ThreadExceptionHandler.class);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        logger.info("Exception collected in Thread {} with message {}", t.getName(), e.getMessage());
        e.printStackTrace();
        ExceptionReporterManager.reportException(new Exception(e), t, true);
    }
}
