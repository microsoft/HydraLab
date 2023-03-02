// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance;

public interface PerformanceResultParser {
    PerformanceTestResult parse(PerformanceTestResult performanceTestResult);

    enum PerformanceResultParserType {
        PARSER_ANDROID_MEMORY_DUMP,
        PARSER_ANDROID_MEMORY_INFO,
        PARSER_ANDROID_BATTERY_INFO,
        PARSER_WIN_BATTERY,
        PARSER_WIN_MEMORY
    }
}
