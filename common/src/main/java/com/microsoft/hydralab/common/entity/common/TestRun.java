// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.ITestRun;
import com.microsoft.hydralab.common.util.Const;
import lombok.Data;
import org.slf4j.Logger;

import javax.persistence.*;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "device_test_task", indexes = {
        @Index(name = "task_id_index", columnList = "test_task_id")})
public class TestRun implements Serializable, ITestRun {
    //    private static Pattern testResultLine = Pattern.compile("Tests run:\\s+(\\d+),\\s+Failures:\\s+(\\d+)");
    // OK (8 tests)
//    private static Pattern testResultOkLine = Pattern.compile("OK\\s+\\((\\d+)\\s+tests\\)");
    // Time: 102.233
//    private static Pattern testResultTestSpentLine = Pattern.compile("Time:\\s+([\\d.]+)");
    @Id
    private String id = UUID.randomUUID().toString();
    @Column(name = "test_task_id")
    private String testTaskId;
    private String deviceSerialNumber;
    private String deviceName;
    /**
     * This is a relative path
     * TODO expects a more general name, a better name could be "logFileRelPath"
     */
    private String instrumentReportPath;
    private String logcatPath;
    private String controlLogPath;
    private String testXmlReportPath;
    private String testGifPath;

    private String crashStackId;
    private String errorInProcess;
    private String deviceTestResultFolderUrl;

    private int totalCount;
    private int failCount;
    private boolean success;
    private long testStartTimeMillis;
    private long testEndTimeMillis;

    @Transient
    private String crashStack;
    @Transient
    private String testErrorMessage;
    @Transient
    private List<AndroidTestUnit> testUnitList = new ArrayList<>();
    @Transient
    private JSONArray videoTimeTagArr = new JSONArray();
    @Transient
    private String videoBlobUrl;
    @Transient
    private List<BlobFileInfo> attachments;

    @Transient
    private transient List<CommandlineAndTime> commandlineAndTimeList = new ArrayList<>();
    @Transient
    private transient File resultFolder;
    @Transient
    private transient Logger logger;

    public TestRun() {
    }

    public TestRun(String deviceSerialNumber, String deviceName, String testTaskId) {
        this.deviceSerialNumber = deviceSerialNumber;
        this.deviceName = deviceName.replace('_', ' ');
        this.testTaskId = testTaskId;
    }

    @Transient
    public String getDisplayTotalTime() {
        float second = (testEndTimeMillis - testStartTimeMillis) / 1000f;
        return String.format("%.2fs", second);
    }

    @Transient
    public String getSuccessRate() {
        if (totalCount == 0) {
            return "0%";
        }
        float rate = 100f * (totalCount - failCount) / totalCount;
        return String.format("%.2f", rate) + '%';
    }

    public void addNewTestUnit(AndroidTestUnit ongoingTestUnit) {
        testUnitList.add(ongoingTestUnit);
    }

    public void addNewTimeTag(String tag, long relTime) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(tag, relTime);
        videoTimeTagArr.add(jsonObject);
    }

    public void addNewTimeTagBeforeLast(String tag, long relTime) {
        int arrLength = videoTimeTagArr.size();
        if (arrLength == 0) {
            addNewTimeTag(tag, relTime);
        } else {
            JSONObject temp = (JSONObject) videoTimeTagArr.remove(arrLength - 1);
            addNewTimeTag(tag, relTime);
            videoTimeTagArr.add(temp);
        }
    }

    public void oneMoreFailure() {
        success = false;
        failCount++;
    }

    public void onTestEnded() {
        testEndTimeMillis = System.currentTimeMillis();
        logger.info("Test end on device {}, fail count: {}, total: {}", deviceName, failCount, totalCount);
        int successCount = (int) testUnitList.stream().filter(u -> u.getStatusCode() == AndroidTestUnit.StatusCodes.OK).count();
        failCount = totalCount - successCount;
        logger.info("After recalc: Test end on device {}, fail count: {}, total: {}", deviceName, failCount, totalCount);
        success = failCount <= 0 && totalCount > 0;
    }

    public String getInstrumentReportBlobUrl() {
        return getBlobUrlStr(instrumentReportPath);
    }

    public String getLogcatBlobUrl() {
        return getBlobUrlStr(logcatPath);
    }

    public String getControlLogBlobUrl() {
        return getBlobUrlStr(controlLogPath);
    }

    public String getTestXmlReportBlobUrl() {
        return getBlobUrlStr(testXmlReportPath);
    }

    public String getTestGifBlobUrl() {
        return getBlobUrlStr(testGifPath);
    }

    public void setVideoBlobUrl() {
        videoBlobUrl = deviceTestResultFolderUrl + "/" + Const.ScreenRecoderConfig.DEFAULT_FILE_NAME;
    }

    private String getBlobUrlStr(String path) {
        if (path == null) {
            return null;
        }
        String[] paths = path.split("/");
        String fileName = paths[paths.length - 1];
        return deviceTestResultFolderUrl + "/" + fileName;
    }

    public static class CommandlineAndTime {
        public String line;
        public long timestamp = System.currentTimeMillis();

        public CommandlineAndTime(String line) {
            this.line = line;
        }

        @Override
        public String toString() {
            return "CommandlineAndTime{" +
                    "line='" + line + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}
