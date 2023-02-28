// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.logger;

public interface LogCollector {
    String start();
    void stopAndAnalyse();
    boolean isCrashFound();
}
