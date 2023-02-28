// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.Entity;

import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class WindowsMemoryParsedData {

    // Process ID to process name.
    private final Map<Long, String> processIdProcessNameMap = new ConcurrentHashMap<>();
    // Process ID to Windows memory metrics.
    private final Map<Long, WindowsMemoryMetrics> processIdWindowsMemoryMetricsMap = new ConcurrentHashMap<>();

    @Data
    public static class WindowsMemoryMetrics {
        private long nonpagedSystemMemorySize64;
        private long pagedMemorySize64;
        private long pagedSystemMemorySize64;
        private long peakPagedMemorySize64;
        private long peakVirtualMemorySize64;
        private long peakWorkingSet64;
        private long privateMemorySize64;
        private long workingSet64;
    }

}