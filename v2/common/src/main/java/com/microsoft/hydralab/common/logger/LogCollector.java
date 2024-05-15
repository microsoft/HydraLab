// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.logger;

public interface LogCollector {
    static final String LOGGER_PREFIX = "logger.devices.";
    String start();
    void stopAndAnalyse();
    boolean isCrashFound();
}
