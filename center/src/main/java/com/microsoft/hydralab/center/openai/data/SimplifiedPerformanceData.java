package com.microsoft.hydralab.center.openai.data;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class SimplifiedPerformanceData {
    @JSONField(name = "name")
    private String name = "";

    @JSONField(name = "value")
    private String value = "";
}
