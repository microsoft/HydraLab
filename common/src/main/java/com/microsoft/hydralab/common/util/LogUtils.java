// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtils {

    /**
     * @param loggerName
     * @param filePath
     * @param logPattern http://logback.qos.ch/manual/layouts.html
     * @return
     */
    public static Logger getLoggerWithRollingFileAppender(String loggerName, String filePath, String logPattern) {
        return getLoggerWithRollingFileAppender(loggerName, filePath, logPattern, null);
    }

    public static Logger getLoggerWithRollingFileAppender(String loggerName, String filePath, String logPattern, Level level) {
        Logger logger = LoggerFactory.getLogger(loggerName);
        if (!(logger instanceof ch.qos.logback.classic.Logger)) {
            throw new RuntimeException("not logback loggers");
        }
        ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) logger;
        String name = "rolling.file.appender";
        if (level != null) {
            logbackLogger.setLevel(level);
        }
        Appender<ILoggingEvent> appender = logbackLogger.getAppender(name);
        if (appender == null) {
            PatternLayoutEncoder ple = new PatternLayoutEncoder();
            // http://logback.qos.ch/manual/appenders.html
            ple.setPattern(logPattern);
            ple.setContext(logbackLogger.getLoggerContext());
            ple.start();

            RollingFileAppender<ILoggingEvent> newAppender = new RollingFileAppender<>();

            TimeBasedRollingPolicy<ILoggingEvent> newRolePolicy = new TimeBasedRollingPolicy<>();

            newRolePolicy.setFileNamePattern(filePath + ".%d");
            newRolePolicy.setMaxHistory(7);
            newRolePolicy.setContext(logbackLogger.getLoggerContext());
            newRolePolicy.setParent(newAppender);
            newRolePolicy.start();

            newAppender.setEncoder(ple);
            newAppender.setContext(logbackLogger.getLoggerContext());
            newAppender.setFile(filePath);
            newAppender.setAppend(true);
            newAppender.setRollingPolicy(newRolePolicy);
            newAppender.setName(name);
            newAppender.start();

            logbackLogger.addAppender(newAppender);
        }
        return logger;
    }

    public static void releaseLogger(Logger logger) {
        if (logger instanceof ch.qos.logback.classic.Logger) {
            ((ch.qos.logback.classic.Logger) logger).detachAndStopAllAppenders();
        }
    }

    public static Boolean isLegalStr(String message, String regex, Boolean nullable) {
        if (StringUtils.isEmpty(message)) {
            return nullable;
        }
        return message.matches(regex);
    }
}
