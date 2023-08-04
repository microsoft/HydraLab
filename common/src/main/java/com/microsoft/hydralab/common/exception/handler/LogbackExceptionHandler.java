// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.exception.handler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.microsoft.hydralab.common.exception.reporter.ExceptionReporterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhoule
 * @date 08/02/2023
 */

public class LogbackExceptionHandler extends AppenderBase<ILoggingEvent> {
    private final Logger logger = LoggerFactory.getLogger(LogbackExceptionHandler.class);

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        logger.warn("Exception collected in Logger {} on thread {} with message {} at {}",
                iLoggingEvent.getLoggerName(),
                iLoggingEvent.getThreadName(),
                iLoggingEvent.getFormattedMessage(),
                iLoggingEvent.getTimeStamp());
        Exception exception = new Exception("Exception collected in Logger " + iLoggingEvent.getLoggerName()
                + " on thread " + iLoggingEvent.getThreadName()
                + " with message " + iLoggingEvent.getFormattedMessage()
                + " at " + iLoggingEvent.getTimeStamp());
        exception.setStackTrace(iLoggingEvent.getCallerData());
        ExceptionReporterManager.reportException(exception, Level.ERROR.equals(iLoggingEvent.getLevel()));
    }
}