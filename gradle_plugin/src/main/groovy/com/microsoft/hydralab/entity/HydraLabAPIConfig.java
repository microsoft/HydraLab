package com.microsoft.hydralab.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// todo: split into APIConfig/deviceConfig/testConfig
public class HydraLabAPIConfig {
    public String schema = "https";
    public String host = "";
    public String contextPath = "";
    public String authToken = "";
    public boolean onlyAuthPost = true;
    public String checkCenterVersionAPIPath = "/api/center/info";
    public String checkCenterAliveAPIPath = "/api/center/isAlive";
    public String getBlobSAS = "/api/package/getSAS";
    public String uploadAPKAPIPath = "/api/package/add";
    public String addAttachmentAPIPath = "/api/package/addAttachment";
    public String generateAccessKeyAPIPath = "/api/deviceGroup/generate?deviceIdentifier=%s";
    public String runTestAPIPath = "/api/test/task/run/";
    public String testStatusAPIPath = "/api/test/task/";
    public String cancelTestTaskAPIPath = "/api/test/task/cancel/%s?reason=%s";
    public String testPortalTaskInfoPath = "/portal/index.html?redirectUrl=/info/task/";
    public String testPortalTaskDeviceVideoPath = "/portal/index.html?redirectUrl=/info/videos/";
    public String pkgName = "";
    public String testPkgName = "";
    public String groupTestType = "SINGLE";
    public String pipelineLink = "";
    public String frameworkType = "JUnit4";
    public int maxStepCount = 100;
    public int deviceTestCount = -1;
    public boolean needUninstall = true;
    public boolean needClearData = true;
    public String teamName = "";
    public String testRunnerName = "androidx.test.runner.AndroidJUnitRunner";
    public String testScope = "";
    public List<String> neededPermissions = new ArrayList<>();
    public String deviceActionsStr = "";

    public String getBlobSASUrl() {
        return String.format(Locale.US, "%s://%s%s%s", schema, host, contextPath, getBlobSAS);
    }

    public String checkCenterAliveUrl() {
        return String.format(Locale.US, "%s://%s%s%s", schema, host, contextPath, checkCenterAliveAPIPath);
    }

    public String getUploadUrl() {
        return String.format(Locale.US, "%s://%s%s%s", schema, host, contextPath, uploadAPKAPIPath);
    }

    public String getAddAttachmentUrl() {
        return String.format(Locale.US, "%s://%s%s%s", schema, host, contextPath, addAttachmentAPIPath);
    }

    public String getGenerateAccessKeyUrl() {
        return String.format(Locale.US, "%s://%s%s%s", schema, host, contextPath, generateAccessKeyAPIPath);
    }

    public String getRunTestUrl() {
        return String.format(Locale.US, "%s://%s%s%s", schema, host, contextPath, runTestAPIPath);
    }

    public String getTestStatusUrl(String testTaskId) {
        return String.format(Locale.US, "%s://%s%s%s%s", schema, host, contextPath, testStatusAPIPath, testTaskId);
    }

    public String getCancelTestTaskUrl() {
        return String.format(Locale.US, "%s://%s%s%s", schema, host, contextPath, cancelTestTaskAPIPath);
    }

    public String getTestReportUrl(String testTaskId) {
        return String.format(Locale.US, "%s://%s%s%s%s", schema, host, contextPath, testPortalTaskInfoPath, testTaskId);
    }

    public String getDeviceTestVideoUrl(String id) {
        return String.format(Locale.US, "%s://%s%s%s%s", schema, host, contextPath, testPortalTaskDeviceVideoPath, id);
    }

    @Override
    public String toString() {
        return "HydraLabAPIConfig:\n" +
                "pkgName=" + pkgName + ",\n" +
                "testPkgName=" + testPkgName + ",\n" +
                "groupTestType=" + groupTestType + ",\n" +
                "pipelineLink=" + pipelineLink + ",\n" +
                "frameworkType=" + frameworkType + ",\n" +
                "maxStepCount=" + maxStepCount + ",\n" +
                "deviceTestCount=" + deviceTestCount + ",\n" +
                "needUninstall=" + needUninstall + ",\n" +
                "needClearData=" + needClearData + ",\n" +
                "teamName=" + teamName + ",\n" +
                "testRunnerName=" + testRunnerName + ",\n" +
                "testScope=" + testScope + ",\n" +
                "neededPermissions=" + (neededPermissions != null ? neededPermissions.toString() : "") + ",\n" +
                "deviceActionsStr=" + deviceActionsStr;
    }
}