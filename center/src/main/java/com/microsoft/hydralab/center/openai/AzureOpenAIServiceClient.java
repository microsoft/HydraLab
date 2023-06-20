package com.microsoft.hydralab.center.openai;

import com.alibaba.fastjson.JSON;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class AzureOpenAIServiceClient {
    private final Logger logger = LoggerFactory.getLogger(AzureOpenAIServiceClient.class);
    private final String apiKey;
    private final String endpoint;
    private final String deployment;
    private final String apiVersion;
    private final OkHttpClient client = new OkHttpClient();

    public AzureOpenAIServiceClient(String apiKey, String deployment, String endpoint, String apiVersion) {
        this.apiKey = apiKey;
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.deployment = deployment;
        this.apiVersion = apiVersion;
    }

    public String chatCompletion(ChatRequest request) {
        MediaType mediaType = MediaType.parse("application/json");
        String url = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s", endpoint, deployment, apiVersion);

        String requestBodyString = JSON.toJSONString(request);
        logger.info("Request body: {}", requestBodyString);

        RequestBody body = RequestBody.create(requestBodyString, mediaType);
        Request httpRequest = new Request.Builder().url(url).post(body)
//                .addHeader("Content-Type", "application/json")
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
