package com.microsoft.hydralab.center.openai;

import com.microsoft.hydralab.center.test.BaseTest;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
    private AzureOpenAIServiceClient azureOpenAIServiceClient;

    @BeforeEach
    public void setUp() {
        if (StringUtils.isBlank(endpoint)) {
            endpoint = "https://your-azure-openai-endpoint.openai.azure.com/";
        }

        azureOpenAIServiceClient =
                new AzureOpenAIServiceClient(apiKey, deployment, endpoint, apiVersion);
    }

    @Test
    public void testChatCompletionWithMock() throws IOException {
        OkHttpClient client = Mockito.mock(OkHttpClient.class);
        Call mockCall = Mockito.mock(Call.class);

        Response response = new Response.Builder()
                .protocol(Protocol.HTTP_1_1).message("testResponse")
                .request(new Request.Builder().url("https://mock.com").build())
                .code(200)
                .body(ResponseBody
                        .create("testResponse", MediaType.parse("application/json"))
                )
                .build();
        when(client.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(response);

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

}
