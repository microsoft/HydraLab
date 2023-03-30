// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.microsoft.hydralab.performance.IBaselineMetrics;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class WindowsMemoryParsedData implements IBaselineMetrics {

    @Override
    @JSONField(serialize = false)
    public LinkedHashMap<String, Double> getBaselineMetricsKeyValue() {
        return null;
    }

    @Override
    @JSONField(serialize = false)
    public SummaryType getSummaryType() {
        return null;
    }

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

    // Process ID to process name.
    private final Map<Long, String> processIdProcessNameMap = new ConcurrentHashMap<>();
    // Process ID to Windows memory metrics.
    private final Map<Long, WindowsMemoryMetrics> processIdWindowsMemoryMetricsMap = new ConcurrentHashMap<>();

}