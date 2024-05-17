// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.center;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import jakarta.persistence.*;

@Entity
@Data
@Table(name = "stability_data")
public class StabilityData {
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "cpu_trace_report")
    private String cpuTraceReport;

    @Column(name = "current_index_num", nullable = false)
    private Integer currentIndexNum;

    @Column(name = "device_test_result_id", nullable = false)
    private String deviceTestResultId;

    @Column(name = "end_time_millis")
    private Long endTimeMillis;

    @Column(name = "memory_dump_report")
    private String memoryDumpReport;

    @Column(name = "numtests", nullable = false)
    private Integer numtests;

    @Column(name = "owner_email")
    private String ownerEmail;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "rel_end_time_in_video")
    private Long relEndTimeInVideo;

    @Column(name = "rel_start_time_in_video")
    private Long relStartTimeInVideo;

    @Column(name = "start_time_millis")
    private Long startTimeMillis;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Column(name = "success", nullable = false)
    private Boolean success = false;

    @Column(name = "test_index")
    private Integer testIndex;

    @Column(name = "test_name")
    private String testName;

    @Column(name = "test_task_id")
    private String testTaskId;

    @Column(name = "tested_class")
    private String testedClass;

    @Column(name = "devicetask_id")
    private String devicetaskId;

    @Column(name = "devicetask_control_log_path")
    private String devicetaskControlLogPath;

    @Column(name = "devicetask_crash_stack_id")
    private String devicetaskCrashStackId;

    @Column(name = "devicetask_device_name")
    private String devicetaskDeviceName;

    @Column(name = "devicetask_device_serial_number")
    private String devicetaskDeviceSerialNumber;

    @Column(name = "devicetask_device_test_result_folder_url")
    private String devicetaskDeviceTestResultFolderUrl;

    @Column(name = "devicetask_error_in_process")
    private String devicetaskErrorInProcess;

    @Column(name = "devicetask_fail_count")
    private Integer devicetaskFailCount;

    @Column(name = "devicetask_instrument_report_path")
    private String devicetaskInstrumentReportPath;

    @Column(name = "devicetask_logcat_path")
    private String devicetaskLogcatPath;

    @Column(name = "devicetask_success")
    private Boolean devicetaskSuccess;

    @Column(name = "devicetask_test_end_time_millis")
    private Long devicetaskTestEndTimeMillis;

    @Column(name = "devicetask_test_gif_path")
    private String devicetaskTestGifPath;

    @Column(name = "devicetask_test_start_time_millis")
    private Long devicetaskTestStartTimeMillis;

    @Column(name = "devicetask_test_task_id")
    private String devicetaskTestTaskId;

    @Column(name = "devicetask_test_xml_report_path")
    private String devicetaskTestXmlReportPath;

    @Column(name = "devicetask_total_count")
    private Integer devicetaskTotalCount;

    @Transient
    public String getTitle() {
        return getTestClassShortName() + "." + testName;
    }

    @JSONField(serialize = false)
    @Transient
    public String getTestClassShortName() {
        return testedClass.substring(testedClass.lastIndexOf('.') + 1);
    }

}