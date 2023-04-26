package com.microsoft.hydralab.performance.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.microsoft.hydralab.performance.IBaselineMetrics;
import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;

@Data
public class IOSMemoryPerfInfo implements Serializable, IBaselineMetrics {
    private String appPackageName;
    @JSONField(name = "timestamp")
    private long timeStamp;
    private String description;
    /**
     * Sample data:
     * memory {'pid': 13862, 'timestamp': 1678877815059, 'value': 67.94049072265625}
     */

    @JSONField(name = "value")
    private float memoryMB;

    @Override
    @JSONField(serialize = false)
    public LinkedHashMap<String, Double> getBaselineMetricsKeyValue() {
        LinkedHashMap<String, Double> baselineMap = new LinkedHashMap<>();
        baselineMap.put("memoryMB", (double) memoryMB);
        return baselineMap;
    }

    @Override
    @JSONField(serialize = false)
    public SummaryType getSummaryType() {
        return SummaryType.AVERAGE;
    }
}
