package com.microsoft.hydralab.center.controller;

import jakarta.annotation.Resource;
import org.springframework.ai.azure.openai.AzureOpenAiChatClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AIChatController {
    @Resource
    AzureOpenAiChatClient azureOpenAiChatClient;
//    @Value("${spring.ai.azure.openai.chat.options.model}")
//    String deploymentName;

    @RequestMapping("/chat")
    public String chat(String message) {
//        azureOpenAiChatClient.getDefaultOptions().setDeploymentName(deploymentName);
        System.out.println(azureOpenAiChatClient.getDefaultOptions().getDeploymentName());
        return azureOpenAiChatClient.call(message);
    }
}
