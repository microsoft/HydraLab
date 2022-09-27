// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.agent;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import lombok.Data;

@Data
public class SmartTestParam {
    public String apkPath;
    public String deviceInfo;
    public String modelInfo;
    public String testSteps;
    public String stringTextFolder;

    public SmartTestParam(String apkPath, DeviceInfo deviceInfo, String sourceModelId, String targetModelId, int testSteps, String folderPath, String stringFolderPath) {
        JSONObject modelInfo = new JSONObject();

        modelInfo.put(Const.SmartTestConfig.bertPathTag, folderPath + Const.SmartTestConfig.bertModelName);
        modelInfo.put(Const.SmartTestConfig.topicPathTag, folderPath + Const.SmartTestConfig.topicModelName);
        modelInfo.put(Const.SmartTestConfig.sourceModelTag, sourceModelId);
        modelInfo.put(Const.SmartTestConfig.targetModelTag, targetModelId);

        this.apkPath = apkPath;
        this.deviceInfo = JSONObject.toJSONString(deviceInfo).replaceAll("\"", "'");
        this.modelInfo = modelInfo.toJSONString().replaceAll("\"", "'");
        this.testSteps = String.valueOf(testSteps);
        this.stringTextFolder = stringFolderPath;
    }
}