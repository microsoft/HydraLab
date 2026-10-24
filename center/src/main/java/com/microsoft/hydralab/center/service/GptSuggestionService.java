package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.openai.AzureOpenAIServiceClient;
import com.microsoft.hydralab.center.openai.ChatMessage;
import com.microsoft.hydralab.center.openai.ChatRequest;

import java.util.Arrays;

public class GptSuggestionService {
    AzureOpenAIServiceClient client;
    String exceptionAnalyzePrompt =
            "        I will give you a test with failure exception information with stack trace.\n" +
            "        I will give you a logcat log which records logs before this exception.\n" +
            "        Do 3 things for me:\n" +
            "            1. Give me a summary about this exception.\n" +
            "            2. Assume root cause of this exception.\n" +
            "            3. Propose potential solution to fix this exception.\n";

    String ExceptionAnalyze(String exceptionStr, String logPath) {
        client = new AzureOpenAIServiceClient(
     "key", "deployment", "endpoint", "api-version");

        String logContent = "";
        ChatRequest req = new ChatRequest();
        req.setMessages(Arrays.asList(
            new ChatMessage("system", exceptionAnalyzePrompt),
            new ChatMessage("user", "exception: " + exceptionStr),
            new ChatMessage("user", "log: " + logContent)
        ));
        return client.chatCompletion(req);
    }
}
