// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.config;

import com.microsoft.hydralab.entity.AttachmentInfo;
import org.gradle.internal.impldep.com.fasterxml.jackson.databind.annotation.JsonAppend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Li Shen
 * @date 2/8/2023
 */

@JsonAppend
public class TestConfig {
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
    public int maxStepCount = 100;
    public int deviceTestCount = -1;
    public boolean needUninstall = true;
    public boolean needClearData = true;
    // priority: config file path in param > direct yml config
    public String attachmentConfigPath = "";
    public List<AttachmentInfo> attachmentInfos = new ArrayList<>();
    public String artifactTag = "";

    public void constructField(HashMap<String, Object> map) {
        Object tag = map.get("tag");
        if (tag != null) {
            this.artifactTag = map.get("tag").toString();
        }
        Object queueTimeOutSeconds = map.get("queueTimeOutSeconds");
        if (queueTimeOutSeconds == null) {
            this.queueTimeOutSeconds = this.runTimeOutSeconds;
        }
    }

    @Override
    public String toString() {
        return "TestConfig:\n" +
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
                "\tmaxStepCount=" + maxStepCount + "\n" +
                "\tdeviceTestCount=" + deviceTestCount + "\n" +
                "\tneedUninstall=" + needUninstall + "\n" +
                "\tneedClearData=" + needClearData + "\n" +
                "\tattachmentConfigPath=" + attachmentConfigPath + "\n" +
                "\tattachmentConfigs=" + attachmentInfos.toString() + "\n" +
                "\tartifactTag=" + artifactTag;
    }
}
