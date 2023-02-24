// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.config;

import java.util.Locale;

public class HydraLabAPIConfig {
    public String schema = "https";
    public String host = "";
    public String contextPath = "";
    public String authToken = "";
    public String checkCenterAliveAPIPath = "/api/center/isAlive";
    public String getStorageTokenAPIPath = "/api/storage/getToken";
    public String uploadAPKAPIPath = "/api/package/add";
    public String addAttachmentAPIPath = "/api/package/addAttachment";
    public String generateAccessKeyAPIPath = "/api/deviceGroup/generate?deviceIdentifier=%s";
    public String runTestAPIPath = "/api/test/task/run/";
    public String testStatusAPIPath = "/api/test/task/";
    public String cancelTestTaskAPIPath = "/api/test/task/cancel/%s?reason=%s";
    public String testPortalTaskInfoPath = "/portal/index.html?redirectUrl=/info/task/";
    public String testPortalTaskDeviceVideoPath = "/portal/index.html?redirectUrl=/info/videos/";

    public String getStorageTokenUrl() {
        return String.format(Locale.US, "%s://%s%s%s", schema, host, contextPath, getStorageTokenAPIPath);
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
                "\tschema=" + schema + "\n" +
                "\thost=" + host;
    }
}