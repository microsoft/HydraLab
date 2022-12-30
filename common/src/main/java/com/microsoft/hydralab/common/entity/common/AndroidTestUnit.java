// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import com.alibaba.fastjson.annotation.JSONField;
import com.microsoft.hydralab.common.util.DateUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.io.File;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(indexes = {
        @Index(name = "task_id_unit_index", columnList = "test_task_id", unique = false),
        @Index(name = "device_test_result_id_index", columnList = "device_test_result_id", unique = false)
})
public class AndroidTestUnit {

    public static final String P_START = "<p style='color:green;font-weight:bold'>";
    @Id
    protected String id = UUID.randomUUID().toString();

    protected boolean success;
    protected int currentIndexNum;
    protected int numtests;
    protected int statusCode;

    protected long startTimeMillis;
    protected long endTimeMillis;
    protected long relStartTimeInVideo;
    protected long relEndTimeInVideo;

    protected String testedClass;
    protected String ownerEmail;
    protected String ownerName;
    @Column(name = "device_test_result_id")
    protected String deviceTestResultId;
    @Column(name = "test_task_id")
    protected String testTaskId;
    protected String testName;
    @Transient
    protected String stack;

    private String memoryDumpReport;
    private String cpuTraceReport;

    @Transient
    private transient File memoryDumpFile;
    @Transient
    private transient File cpuTraceFile;

    @Transient
    private TestRun testRun;

    @Transient
    public String getTitle() {
        return getTestClassShortName() + "." + testName;
    }

    @JSONField(serialize = false)
    @Transient
    public String getTestClassShortName() {
        return testedClass.substring(testedClass.lastIndexOf('.') + 1);
    }

    @JSONField(serialize = false)
    @Transient
    public String getKey() {
        return currentIndexNum + getTestClassShortName() + testName;
    }

    /**
     * Copied from {@link com.android.ddmlib.testrunner.InstrumentationResultParser} private class StatusCodes
     * The original constants are inaccessible.
     */
    public interface StatusCodes {
        int START = 1;
        int IN_PROGRESS = 2;

        // codes used for test completed
        int ASSUMPTION_FAILURE = -4;
        int IGNORED = -3;
        int FAILURE = -2;
        int ERROR = -1;
        int OK = 0;
    }

    public String getStatusDesc() {
        switch (statusCode) {
            case StatusCodes.OK:
                return "OK";
            case StatusCodes.FAILURE:
                return "Failure";
            case StatusCodes.ASSUMPTION_FAILURE:
                return "Assumption failure";
            case StatusCodes.ERROR:
                return "Error";
            case StatusCodes.IGNORED:
                return "Ignored";
            case StatusCodes.IN_PROGRESS:
                return "In progress";
            case StatusCodes.START:
                return "Start";
            default:
                return "Unknown";
        }
    }

    @Transient
    public String getDisplaySpentTime() {
        float second = (endTimeMillis - startTimeMillis) / 1000f;
        return String.format("%.2fs", second);
    }

    @Transient
    public String getDisplayRelStartTimeInVideo() {
        return DateUtil.mmssFormat.format(new Date(relStartTimeInVideo));
    }

    @Transient
    public String getDisplayRelEndTimeInVideo() {
        return DateUtil.mmssFormat.format(new Date(relEndTimeInVideo));
    }

    @JSONField(serialize = false)
    @Transient
    public String getStackHtml() {
        if (StringUtils.isBlank(stack)) {
            return "";
        }
        return stack.replace("\n", "<br>");
    }

}
