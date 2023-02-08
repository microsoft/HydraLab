// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.config;

import com.microsoft.hydralab.entity.AttachmentInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Li Shen
 * @date 2/8/2023
 */

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
    public int runTimeOutSeconds = -1;
    public int queueTimeOutSeconds = -1;
    public String pipelineLink = "";
    public int maxStepCount = 100;
    public int deviceTestCount = -1;
    public boolean needUninstall = true;
    public boolean needClearData = true;
    // priority: config file path in param > yml config
    public String attachmentConfigPath = "";
    public List<AttachmentInfo> attachmentConfigs = new ArrayList<>();
    public String artifactTag = "";

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
                "\tattachmentConfigs=" + attachmentConfigs.toString() + "\n" +
                "\tartifactTag=" + artifactTag;
    }
}
