package com.microsoft.hydralab.center.controller;

import jakarta.annotation.Resource;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AIChatController {
//    @Value("${spring.ai.azure.openai.chat.options.model}")
//    String deploymentName;
    @Resource
    private AzureOpenAiChatModel azureOpenAiChatModel;
    @RequestMapping("/chat")
    public String chat(String message) {
        return azureOpenAiChatModel.call(message);
    }
}
