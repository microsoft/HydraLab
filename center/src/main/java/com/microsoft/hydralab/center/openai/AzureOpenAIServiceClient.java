package com.microsoft.hydralab.center.openai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
public class AzureOpenAIServiceClient {
    private final Logger logger = LoggerFactory.getLogger(AzureOpenAIServiceClient.class);
    private final String apiKey;
    private final String endpoint;
    private final String deployment;
    private final String apiVersion;
    OkHttpClient client = new OkHttpClient();

    public AzureOpenAIServiceClient(String apiKey, String deployment, String endpoint, String apiVersion) {
        this.apiKey = apiKey;
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.deployment = deployment;
        this.apiVersion = apiVersion;
    }

    public String chatCompletion(ChatRequest request) {
        return callAzureOpenAIAPI("chat/completions", JSON.toJSONString(request));
    }

    private String callAzureOpenAIAPI(String operation, String requestBodyString) {
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

    String callAzureOpenAIImageAPI(String prompt, int number, String size, String apiVersion) {
        MediaType mediaType = MediaType.parse("application/json");
        String url = String.format("%s/openai/images/generations:submit?api-version=%s", endpoint, apiVersion);

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

    String getGeneratedImageStatus(String id, String apiVersion) {
        String url = String.format("%s/openai/operations/images/%s?api-version=%s", endpoint, id, apiVersion);
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