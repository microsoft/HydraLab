package com.microsoft.hydralab.center.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Result;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

class LongChainExampleTest {
    // Get this from https://platform.openai.com/account/api-keys
    String openAIApiKey;
    ChatLanguageModel model;

    {
        String name = "env.properties";
        if (new File(name).exists()) {
            Properties properties = new Properties();
            try {
                properties.load(new FileInputStream(name));
                openAIApiKey = properties.getProperty("OPENAI_TOKEN");
                if (!StringUtils.isBlank(openAIApiKey)) {
                    model = OpenAiChatModel.withApiKey(openAIApiKey);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void testChain() {
        // cannot do this test without a valid openai api key and initialized model.
        if (model == null) {
            return;
        }

        LongChainExample.MaestroCaseGeneration prompt = new LongChainExample.MaestroCaseGeneration();
        Document document;
        SAXReader saxReader = new SAXReader();
        try {
            document = saxReader.read("src/test/resources/test_route_map.xml");
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
        prompt.maestroExample = "appId: {the id of test app }\n" +
                "---\n" +
                "- launchApp\n" +
                "- tapOn: \"Create new contact\"\n" +
                "- tapOn: \"First name\"\n" +
                "- inputRandomPersonName\n" +
                "- tapOn: \"Last name\"\n" +
                "- inputRandomPersonName\n" +
                "- tapOn: \"Phone\"\n" +
                "- inputRandomNumber:\n" +
                "    length: 10\n" +
                "- back\n" +
                "- tapOn: \"Email\"\n" +
                "- inputRandomEmail\n" +
                "- tapOn: \"Save\"";
        prompt.routeMap = document.asXML();

        Result<AiMessage> result = model.sendUserMessage(prompt);
        System.out.println(result.get().text());
    }

}