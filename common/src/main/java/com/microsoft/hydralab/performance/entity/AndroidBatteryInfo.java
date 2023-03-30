// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance.entity;

import com.microsoft.hydralab.performance.IBaselineMetrics;
import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;

@Data
public class AndroidBatteryInfo implements Serializable, IBaselineMetrics {
    private String appPackageName;
    private long timeStamp;
    private float cpu;
    private float screen;
    private float wakeLock;
    private float systemService;
    private float wifi;
    private float appUsage;
    private float total;
    private float ratio;
    private String description;

    @Override
    public LinkedHashMap<String, Double> getBaselineMetricsKeyValue() {
        LinkedHashMap<String, Double> baselineMap = new LinkedHashMap<>();
        baselineMap.put("total", (double) total);
        baselineMap.put("appUsage", (double) appUsage);
        baselineMap.put("cpu", (double) cpu);
        baselineMap.put("systemService", (double) systemService);
        baselineMap.put("wakeLock", (double) wakeLock);
        return baselineMap;
    }

    @Override
    public SummaryType getSummaryType() {
        return SummaryType.MAX;
    }
}
