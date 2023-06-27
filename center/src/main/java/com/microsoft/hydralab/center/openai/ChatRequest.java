package com.microsoft.hydralab.center.openai;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    private List<ChatMessage> messages;
    @JSONField(name = "max_tokens")
    private int maxTokens = 800;
    private double temperature = 0.75;
    @JSONField(name = "frequency_penalty")
    private double frequencyPenalty = 0;
    @JSONField(name = "presence_penalty")
    private double presencePenalty = 0;
    @JSONField(name = "top_p")
    private double topP = 0.95;
    private String stop;
}
