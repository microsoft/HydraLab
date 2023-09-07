package com.microsoft.hydralab.center.openai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.microsoft.hydralab.center.openai.data.ChatRequest;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

    // Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
    public class AzureOpenAIServiceClient {
    public static final String API_VERSION_CHAT = "2023-03-15-preview";
    public static final String API_VERSION_IMAGE = "2023-06-01-preview";
    private final Logger logger = LoggerFactory.getLogger(AzureOpenAIServiceClient.class);
    private final String apiKey;
    private final String endpoint;
    private final String deployment;
    OkHttpClient client = new OkHttpClient();
    private OpenAIClient azureClient;

    public AzureOpenAIServiceClient(String apiKey, String deployment, String endpoint) {
        this.apiKey = apiKey == null ? "" : apiKey;
        this.endpoint = endpoint == null ? "" : endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.deployment = deployment == null ? "" : deployment;
        this.azureClient = new OpenAIClientBuilder()
            .endpoint(endpoint)
            .credential(new AzureKeyCredential(apiKey))
            .buildClient();
    }

    public String completion(String question) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage(ChatRole.SYSTEM, "You are a helpful assistant."));
        chatMessages.add(new ChatMessage(ChatRole.USER, question));

        ChatCompletionsOptions options = new ChatCompletionsOptions(chatMessages);
        options.setN(1);

        ChatCompletions chatCompletions = azureClient.getChatCompletions(deployment, options);

        for (ChatChoice choice : chatCompletions.getChoices()) {
            return choice.getMessage().getContent();
        }
        return "";
    }

    public String chatCompletion(ChatRequest request) {
        return callAzureOpenAIAPI("chat/completions", JSON.toJSONString(request), API_VERSION_CHAT);
    }

    private String callAzureOpenAIAPI(String operation, String requestBodyString, String apiVersion) {
        MediaType mediaType = MediaType.parse("application/json");
        String url = String.format("%s/openai/deployments/%s/%s?api-version=%s", endpoint, deployment, operation, apiVersion);
        logger.info("Request body: {}", requestBodyString);
        RequestBody body = RequestBody.create(requestBodyString, mediaType);
        Request httpRequest = new Request.Builder().url(url).post(body)
                .addHeader("api-key", apiKey).build();
        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unexpected response code: " + response);
            }
            return Objects.requireNonNull(response.body()).string();
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while invoking Azure OpenAI API: " + e.getMessage(), e);
        }
    }

    /**
     * See API doc in:
     * <a href="https://learn.microsoft.com/en-us/azure/cognitive-services/openai/dall-e-quickstart?pivots=rest-api">API</a>
     */
    String callAzureOpenAIImageAPI(String prompt, int number, String size) {
        MediaType mediaType = MediaType.parse("application/json");
        String url = String.format("%s/openai/images/generations:submit?api-version=%s", endpoint, API_VERSION_IMAGE);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("prompt", prompt);
        jsonObject.put("n", number);
        jsonObject.put("size", size);

        String jsonString = jsonObject.toJSONString();
        logger.info("Request body: {}", jsonString);

        RequestBody body = RequestBody.create(jsonString, mediaType);
        Request httpRequest = new Request.Builder().url(url).post(body)
                .addHeader("api-key", apiKey).build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unexpected response code: " + response);
            }
            return Objects.requireNonNull(response.body()).string();
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while invoking Azure OpenAI API: " + e.getMessage(), e);
        }
    }

    String getGeneratedImageStatus(String id) {
        String url = String.format("%s/openai/operations/images/%s?api-version=%s", endpoint, id, API_VERSION_IMAGE);
        Request httpRequest = new Request.Builder().url(url)
                .addHeader("api-key", apiKey).build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unexpected response code: " + response);
            }
            return Objects.requireNonNull(response.body()).string();
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while invoking Azure OpenAI API: " + e.getMessage(), e);
        }
    }
}