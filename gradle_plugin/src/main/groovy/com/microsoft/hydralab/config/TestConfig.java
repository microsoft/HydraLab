// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.hydralab.entity.AttachmentInfo;
import com.microsoft.hydralab.entity.performance.InspectionStrategy;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.hydralab.utils.CommonUtils.GSON;

/**
 * @author Li Shen
 * @date 2/8/2023
 */

public class TestConfig {
    public String triggerType = "API";
    @JsonProperty("device")
    public DeviceConfig deviceConfig = new DeviceConfig();
    public String runningType = "";
    public String appPath = "";
    public String testAppPath = "";
    public String pkgName = "";
    public String testPkgName = "";
    public String teamName = "";
    public String testRunnerName = "androidx.test.runner.AndroidJUnitRunner";
    public String testScope = "";
    public String testSuiteName = "";
    public String frameworkType = "JUnit4";
    public int runTimeOutSeconds = 0;
    public int queueTimeOutSeconds = 0;
    public String pipelineLink = "";
    public boolean needUninstall = true;
    public boolean needClearData = true;
    public List<String> neededPermissions = new ArrayList<>();
    // priority: config file path in param > direct yml config
    public String attachmentConfigPath = "";
    public List<AttachmentInfo> attachmentInfos = new ArrayList<>();
    public String artifactTag = "";
    public Map<String, String> testRunArgs;
    public int maxStepCount = 100;
    public int testRound = -1;
    public List<InspectionStrategy> inspectionStrategies = new ArrayList<>();
    public String inspectionStrategiesStr = "";

    public void constructField(HashMap<String, Object> map) {
        Object queueTimeOutSeconds = map.get("queueTimeOutSeconds");
        if (queueTimeOutSeconds == null) {
            this.queueTimeOutSeconds = this.runTimeOutSeconds;
        }
        HashMap<String, Object> explorationArgs = (HashMap<String, Object>)map.get("exploration");
        Object maxStepCount = explorationArgs.get("maxStepCount");
        if (maxStepCount != null) {
            this.maxStepCount = Integer.parseInt(maxStepCount.toString());
        }
        Object testRound = explorationArgs.get("testRound");
        if (testRound != null) {
            this.testRound = Integer.parseInt(testRound.toString());
        }
    }

    public void extractFromExistingField(){
        if (StringUtils.isBlank(this.inspectionStrategiesStr) && this.inspectionStrategies.size() != 0) {
            this.inspectionStrategiesStr = GSON.toJson(this.inspectionStrategies);
        }
    }

    @Override
    public String toString() {
        return "TestConfig:\n" +
                "\t" + deviceConfig.toString() + "\n" +
                "\ttriggerType=" + triggerType + "\n" +
                "\trunningType=" + runningType + "\n" +
                "\tappPath=" + appPath + "\n" +
                "\ttestAppPath=" + testAppPath + "\n" +
                "\tpkgName=" + pkgName + "\n" +
                "\ttestPkgName=" + testPkgName + "\n" +
                "\tteamName=" + teamName + "\n" +
                "\ttestRunnerName=" + testRunnerName + "\n" +
                "\ttestScope=" + testScope + "\n" +
                "\ttestSuiteName=" + testSuiteName + "\n" +
                "\tframeworkType=" + frameworkType + "\n" +
                "\trunTimeOutSeconds=" + runTimeOutSeconds + "\n" +
                "\tqueueTimeOutSeconds=" + queueTimeOutSeconds + "\n" +
                "\tpipelineLink=" + pipelineLink + "\n" +
                "\tneedUninstall=" + needUninstall + "\n" +
                "\tneedClearData=" + needClearData + "\n" +
                "\tneededPermissions=" + (neededPermissions != null ? neededPermissions.toString() : "") + "\n" +
                "\tattachmentConfigPath=" + attachmentConfigPath + "\n" +
                "\tattachmentConfigs=" + attachmentInfos.toString() + "\n" +
                "\tartifactTag=" + artifactTag + "\n" +
                "\ttestRunArgs=" + testRunArgs + "\n" +
                "\tmaxStepCount=" + maxStepCount + "\n" +
                "\ttestRound=" + testRound + "\n" +
                "\tinspectionStrategiesStr=" + inspectionStrategiesStr;
    }
}
