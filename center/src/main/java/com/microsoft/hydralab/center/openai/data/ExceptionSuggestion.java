package com.microsoft.hydralab.center.openai.data;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class ExceptionSuggestion {
    @JSONField(name = "content")
    private String content = "";
}
