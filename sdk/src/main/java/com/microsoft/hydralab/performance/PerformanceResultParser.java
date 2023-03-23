// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance;

public interface PerformanceResultParser {
    PerformanceTestResult parse(PerformanceTestResult performanceTestResult);

    enum PerformanceResultParserType {
        PARSER_ANDROID_MEMORY_DUMP() {
            @Override
            public String[] getBaselineKeys() {
                return new String[0];
            }
        },
        PARSER_ANDROID_MEMORY_INFO {
            @Override
            public String[] getBaselineKeys() {
                return new String[0];
            }
        },
        PARSER_ANDROID_BATTERY_INFO {
            @Override
            public String[] getBaselineKeys() {
                return new String[0];
            }
        },
        PARSER_WIN_BATTERY {
            @Override
            public String[] getBaselineKeys() {
                return new String[0];
            }
        },
        PARSER_WIN_MEMORY {
            @Override
            public String[] getBaselineKeys() {
                return new String[0];
            }
        };

        public abstract String[] getBaselineKeys();
    }
}
