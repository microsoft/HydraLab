// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.agent;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.util.Const;
import lombok.Data;

@Data
public class SmartTestParam {
    @SuppressWarnings("VisibilityModifier")
    public String apkPath;
    @SuppressWarnings("VisibilityModifier")
    public String deviceInfo;
    @SuppressWarnings("VisibilityModifier")
    public String modelInfo;
    @SuppressWarnings("VisibilityModifier")
    public String testSteps;
    @SuppressWarnings("VisibilityModifier")
    public String stringTextFolder;

    public SmartTestParam(String apkPath, DeviceInfo deviceInfo, String sourceModelId, String targetModelId, int testSteps, String folderPath, String stringFolderPath) {
        JSONObject modelInfo = new JSONObject();

        modelInfo.put(Const.SmartTestConfig.BERT_PATH_TAG, folderPath + Const.SmartTestConfig.BERT_MODEL_NAME);
        modelInfo.put(Const.SmartTestConfig.TOPIC_PATH_TAG, folderPath + Const.SmartTestConfig.TOPIC_MODEL_NAME);
        modelInfo.put(Const.SmartTestConfig.SOURCE_MODEL_TAG, sourceModelId);
        modelInfo.put(Const.SmartTestConfig.TARGET_MODEL_TAG, targetModelId);

        this.apkPath = apkPath;
        this.deviceInfo = JSONObject.toJSONString(deviceInfo).replaceAll("\"", "'");
        this.modelInfo = modelInfo.toJSONString().replaceAll("\"", "'");
        this.testSteps = String.valueOf(testSteps);
        this.stringTextFolder = stringFolderPath;
    }
}