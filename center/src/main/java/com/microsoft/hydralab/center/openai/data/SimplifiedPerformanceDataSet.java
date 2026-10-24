package com.microsoft.hydralab.center.openai.data;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.Map;

@Data
public class SimplifiedPerformanceDataSet {
    @JSONField(name = "timestamp")
    private long timestamp;
    @JSONField(name = "inspect")
    private Map<String, Object> inspect;
}
