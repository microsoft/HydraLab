// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.entity;

import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class WindowsMemoryParsedData {

    @Data
    public static class WindowsMemoryMetrics
    {
        private long nonpagedSystemMemorySize64;
        private long pagedMemorySize64;
        private long pagedSystemMemorySize64;
        private long peakPagedMemorySize64;
        private long peakVirtualMemorySize64;
        private long peakWorkingSet64;
        private long privateMemorySize64;
        private long workingSet64;

        public void accumulate(WindowsMemoryMetrics metrics)
        {
            if (metrics == null) {
                return;
            }

            this.nonpagedSystemMemorySize64 += metrics.nonpagedSystemMemorySize64;
            this.pagedMemorySize64 += metrics.pagedMemorySize64;
            this.pagedSystemMemorySize64 += metrics.pagedSystemMemorySize64;
            this.peakPagedMemorySize64 += metrics.peakPagedMemorySize64;
            this.peakVirtualMemorySize64 += metrics.peakVirtualMemorySize64;
            this.peakWorkingSet64 += metrics.peakWorkingSet64;
            this.privateMemorySize64 += metrics.privateMemorySize64;
            this.workingSet64 += metrics.workingSet64;
        }

        public void dividedBy(long divisor)
        {
            if (divisor <= 0) {
                throw new ArithmeticException("The divisor cannot be less than or equal to zero.");
            }

            this.nonpagedSystemMemorySize64 /= divisor;
            this.pagedMemorySize64 /= divisor;
            this.pagedSystemMemorySize64 /= divisor;
            this.peakPagedMemorySize64 /= divisor;
            this.peakVirtualMemorySize64 /= divisor;
            this.peakWorkingSet64 /= divisor;
            this.privateMemorySize64 /= divisor;
            this.workingSet64 /= divisor;
        }
    }

    // Process ID to process name.
    private final Map<Long, String> processIdProcessNameMap = new ConcurrentHashMap<>();
    // Process ID to Windows memory metrics.
    private final Map<Long, WindowsMemoryMetrics> processIdWindowsMemoryMetricsMap = new ConcurrentHashMap<>();

}