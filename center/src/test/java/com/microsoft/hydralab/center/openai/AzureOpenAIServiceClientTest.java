package com.microsoft.hydralab.center.openai;

import com.microsoft.hydralab.center.test.BaseTest;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
public class AzureOpenAIServiceClientTest extends BaseTest {
    @Value("${app.openai.chat-completion.api-key:}")
    private String apiKey;
    @Value("${app.openai.chat-completion.endpoint-url:}")
    private String endpoint;
    @Value("${app.openai.chat-completion.deployment:}")
    private String deployment;
    @Value("${app.openai.chat-completion.api-version:}")
    private String apiVersion;

    @Test
    public void createAzureOpenAIServiceClientAndAsk() {
        if (StringUtils.isBlank(apiKey)) {
            return;
        }
        AzureOpenAIServiceClient azureOpenAIServiceClient = new AzureOpenAIServiceClient(apiKey, deployment, endpoint, apiVersion);
        ChatRequest request = new ChatRequest();
        request.setMessages(Arrays.asList(
                new ChatMessage(ChatMessage.Role.SYSTEM, "You are an AI assistant that helps people find information."),
                new ChatMessage(ChatMessage.Role.USER, "Could you tell me a joke?")
        ));
        baseLogger.info(azureOpenAIServiceClient.chatCompletion(request));
    }
}
