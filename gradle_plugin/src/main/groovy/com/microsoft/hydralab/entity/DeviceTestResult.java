// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.entity;

import java.util.List;

public class DeviceTestResult {
    public String id;
    public String deviceSerialNumber;
    public String deviceName;
    public String instrumentReportPath;
    public String controlLogPath;
    public String instrumentReportBlobUrl;
    public String testXmlReportBlobUrl;
    public String logcatBlobUrl;
    public String testGifBlobUrl;

    public List<BlobFileInfo> attachments;

    public String crashStackId;
    public String errorInProcess;

    public String crashStack;

    public int totalCount;
    public int failCount;
    public boolean success;
    public long testStartTimeMillis;
    public long testEndTimeMillis;

    @Override
    public String toString() {
        return "{" +
                "SN='" + deviceSerialNumber + '\'' +
                ", totalCase:" + totalCount +
                ", failCase:" + failCount +
                ", success:" + success +
                '}';
    }
}