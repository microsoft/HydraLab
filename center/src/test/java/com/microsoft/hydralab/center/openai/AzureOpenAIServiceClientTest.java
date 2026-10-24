// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.openai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.center.openai.data.ChatMessage;
import com.microsoft.hydralab.center.openai.data.ChatRequest;
import com.microsoft.hydralab.center.test.BaseTest;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AzureOpenAIServiceClientTest extends BaseTest {
    @Value("${app.openai.chat-completion.api-key:}")
    private String apiKey;
    @Value("${app.openai.chat-completion.endpoint-url:}")
    private String endpoint;
    @Value("${app.openai.chat-completion.deployment:}")
    private String deployment;
    @Value("${app.openai.chat-completion.api-version:}")
    private String apiVersion;
    private AzureOpenAIServiceClient azureOpenAIServiceClient;

    @BeforeEach
    public void setUp() {
        if (StringUtils.isBlank(endpoint)) {
            endpoint = "https://your-azure-openai-endpoint.openai.azure.com/";
        }

        azureOpenAIServiceClient =
                new AzureOpenAIServiceClient(apiKey, deployment, endpoint);
    }

    @Test
    public void testChatCompletionWithMock() throws IOException {
        OkHttpClient client = mockOkHttpClient("testResponse");

        ChatRequest request = new ChatRequest();
        request.setMessages(Arrays.asList(
                new ChatMessage(ChatMessage.Role.SYSTEM, "You are an AI assistant that helps people find information."),
                new ChatMessage(ChatMessage.Role.USER, "Could you tell me a joke?")
        ));

        azureOpenAIServiceClient.client = client;

        String result = azureOpenAIServiceClient.chatCompletion(request);
        assertEquals("testResponse", result);
    }

    @Test
    public void testAzureOpenAIServiceClientChatCompletion() {
        if (StringUtils.isBlank(apiKey)) {
            return;
        }
        ChatRequest request = new ChatRequest();
        request.setMessages(Arrays.asList(
                new ChatMessage(ChatMessage.Role.SYSTEM, "You are an AI assistant that helps people find information."),
                new ChatMessage(ChatMessage.Role.USER, "Could you tell me a joke?")
        ));
        baseLogger.info(azureOpenAIServiceClient.chatCompletion(request));
    }

    @Test
    public void testAzureOpenAIServiceClientImageAPI() {
        if (StringUtils.isBlank(apiKey)) {
            return;
        }
        String response = azureOpenAIServiceClient.callAzureOpenAIImageAPI(
                "Draw a picture of a real high-resolution apple iphone with a hand drawing Microsoft icon on it.",
                1, "1024x1024");
        baseLogger.info(response);
        JSONObject jsonObject = JSON.parseObject(response);
        String id = jsonObject.getString("id");

        String status = "";
        int maxRetry = 20;
        int counter = 0;
        while (!Objects.equals(status, "succeeded") && counter++ < maxRetry) {
            response = azureOpenAIServiceClient.getGeneratedImageStatus(id);
            jsonObject = JSON.parseObject(response);
            status = jsonObject.getString("status");
            baseLogger.info(response);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        baseLogger.info(response);
    }

}
