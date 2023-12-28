// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author zhoule
 * @date 12/25/2023
 */
@Data
@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Inheritance(strategy = InheritanceType.JOINED)
public class TaskResult implements Serializable {
    @Id
    private String id;
    private String taskRunId;
    private String taskId;
    private String state;
    private Date ingestTime;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private List<String> reportFiles = new ArrayList<>();


    public TaskResult() {
        this.id = UUID.randomUUID().toString();
        ingestTime = new Date();
    }

    public void addReportFile(String reportFile) {
        reportFiles.add(reportFile);
    }
    public enum TaskState {
        // The test passed
        PASS,
        // The test failed
        FAIL,
        /**
         * The test was skipped.
         * Skipped tests typically occur when tests are available for optional functionality. For example, a
         * test suite for video cards might include tests that run only if certain features are enabled in
         * hardware.
         */
        SKIP,
        /**
         * The test was aborted.
         * Aborted tests typically occur when a test suite is interrupted by a user or a system.
         * This is also known as a "ABORT".
         *
         * Another most common example of an aborted result occurs when expected support files are not
         * available. For example, if test collateral (additional files needed to run the test) are located on
         * a network share and that share is unavailable, the test is aborted.
         */
        CANCEL,
        /**
         * The test was blocked from running by a known application or system issue. Marking tests as
         * blocked (rather than failed) when they cannot run as a result of a known bug keeps failure
         * rates from being artificially high, but high numbers of blocked test cases indicate areas where
         * quality and functionality are untested and unknown.
         */
        BLOCK,
        /**
         * The test passed but indicated warnings that might need to be examined in greater detail.
         */
        WARNING
    }
}
