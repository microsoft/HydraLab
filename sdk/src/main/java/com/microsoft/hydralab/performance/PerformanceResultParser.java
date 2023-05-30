// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance;

import org.slf4j.Logger;

public interface PerformanceResultParser {
    PerformanceTestResult parse(PerformanceTestResult performanceTestResult, Logger logger);

    enum PerformanceResultParserType {
        PARSER_ANDROID_MEMORY_DUMP,
        PARSER_ANDROID_MEMORY_INFO,
        PARSER_ANDROID_BATTERY_INFO,
        PARSER_WIN_BATTERY,
        PARSER_WIN_MEMORY,
        PARSER_IOS_ENERGY,
        PARSER_IOS_MEMORY,
        PARSER_EVENT_TIME
    }
}
