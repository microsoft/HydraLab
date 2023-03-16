package com.microsoft.hydralab.performance.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

@Data
public class IOSMemoryPerfInfo implements Serializable {
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
}
