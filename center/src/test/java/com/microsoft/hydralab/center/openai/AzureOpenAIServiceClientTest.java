package com.microsoft.hydralab.center.openai;

import com.microsoft.hydralab.center.test.BaseTest;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;

@ActiveProfiles("local")
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
        AzureOpenAIServiceClient azureOpenAIServiceClient = new AzureOpenAIServiceClient(apiKey, endpoint, deployment, apiVersion);
        ChatRequest request = new ChatRequest();
        request.setMessages(Arrays.asList(
                new ChatMessage(ChatMessage.Role.SYSTEM, "You are an AI assistant that helps people find information."),
                new ChatMessage(ChatMessage.Role.USER, "Could you tell me a joke?")
        ));
        System.out.println(azureOpenAIServiceClient.chatCompletion(request));
    }
}
