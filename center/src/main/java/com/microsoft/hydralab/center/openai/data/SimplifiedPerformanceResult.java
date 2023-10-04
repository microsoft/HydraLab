package com.microsoft.hydralab.center.openai.data;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class SimplifiedPerformanceResult {
    @JSONField(name = "type")
    private String type = "";

    @JSONField(name = "dataset")
    private List<SimplifiedPerformanceDataSet> dataset;
}
