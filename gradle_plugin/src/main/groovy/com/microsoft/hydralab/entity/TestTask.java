// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.entity;

import java.util.Date;
import java.util.List;

public class TestTask {
    public String id;
    public List<DeviceTestResult> deviceTestResults;
    public int testDevicesCount;
    public Date startDate;
    public Date endDate;
    public int totalTestCount;
    public int totalFailCount;
    public String testSuite;
    public String reportImagePath;
    public String status;
    public String testErrorMsg;
    public String message;
    public int retryTime;
    public String blockedDeviceSerialNumber;
    public String unblockedDeviceSerialNumber;
    public String unblockDeviceSecretKey;

    @Override
    public String toString() {
        return "TestTask{" +
                "id='" + id + '\'' +
                ", testDevicesCount=" + testDevicesCount +
                ", startDate=" + startDate +
                ", totalTestCount=" + totalTestCount +
                ", status='" + status + '\'' +
                '}';
    }

    public interface TestStatus {
        String RUNNING = "running";
        String FINISHED = "finished";
        String CANCELED = "canceled";
        String EXCEPTION = "error";
        String WAITING = "waiting";
    }
}
