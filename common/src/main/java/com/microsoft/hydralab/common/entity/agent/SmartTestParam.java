// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.agent;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestAppContext;
import com.microsoft.hydralab.common.util.Const;
import lombok.Data;

import java.io.File;

@Data
public class SmartTestParam {
    public String apkPath;
    public DeviceInfo deviceInfo;
    public JSONObject modelInfo;
    public String testSteps;
    public String stringTextFolder;
    public File outputFolder;
    public JSONObject llmInfo;
    public TestAppContext testAppContext;

    public SmartTestParam(String apkPath,
                          DeviceInfo deviceInfo,
                          String sourceModelId,
                          String targetModelId,
                          int testSteps,
                          String folderPath,
                          String stringFolderPath,
                          File outputFolder,
                          LLMProperties llmProperties,
                          TestAppContext testAppContext) {
        JSONObject modelInfo = new JSONObject();
        modelInfo.put(Const.SmartTestConfig.BERT_PATH_TAG, folderPath + Const.SmartTestConfig.BERT_MODEL_NAME);
        modelInfo.put(Const.SmartTestConfig.TOPIC_PATH_TAG, folderPath + Const.SmartTestConfig.TOPIC_MODEL_NAME);
        modelInfo.put(Const.SmartTestConfig.SOURCE_MODEL_TAG, sourceModelId);
        modelInfo.put(Const.SmartTestConfig.TARGET_MODEL_TAG, targetModelId);

        JSONObject llmInfo = new JSONObject();
        llmInfo.put(Const.SmartTestConfig.LLM_ENABLE, llmProperties.getEnabled());
        llmInfo.put(Const.SmartTestConfig.LLM_DEPLOYMENT, llmProperties.getDeploymentName());
        llmInfo.put(Const.SmartTestConfig.LLM_API_KEY, llmProperties.getOpenaiApiKey());
        llmInfo.put(Const.SmartTestConfig.LLM_API_BASE, llmProperties.getOpenaiApiBase());
        llmInfo.put(Const.SmartTestConfig.LLM_API_VERSION, llmProperties.getOpenaiApiVersion());

        this.apkPath = apkPath;
        this.deviceInfo = deviceInfo;
        this.modelInfo = modelInfo;
        this.testSteps = String.valueOf(testSteps);
        this.stringTextFolder = stringFolderPath;
        this.outputFolder = outputFolder;
        this.llmInfo = llmInfo;
        this.testAppContext = testAppContext;
    }

    public String toJSONString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("apkPath", apkPath);
        jsonObject.put("deviceInfo", deviceInfo);
        jsonObject.put("modelInfo", modelInfo);
        jsonObject.put("testSteps", testSteps);
        jsonObject.put("stringTextFolder", stringTextFolder);
        jsonObject.put("outputFolder", outputFolder.getAbsolutePath());
        jsonObject.put("llmInfo", llmInfo);
        jsonObject.put("appContext", testAppContext);

        return jsonObject.toJSONString();
    }
}