// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.sql.Date;
import java.util.UUID;

/**
 * @author taoran
 * @date 3/14/2023
 */
@Data
@Entity
public class PerformanceTestResultEntity implements Serializable {
    @Id
    private String id = UUID.randomUUID().toString();
    private String testRunId;
    private String testTaskId;
    private Date date = new Date(System.currentTimeMillis());
    private String inspectorType;
    private String parserType;
    private String summaryJSON;
    private String testSuite;
    private String runningType;
    private String appId;
    private String deviceId;
    private boolean success;

    public PerformanceTestResultEntity(String testRunId, String testTaskId, String inspectorType, String parserType, String summaryJSON, String testSuite,
                                       String runningType, String appId, String deviceId, boolean success) {
        this.testRunId = testRunId;
        this.testTaskId = testTaskId;
        this.inspectorType = inspectorType;
        this.parserType = parserType;
        this.summaryJSON = summaryJSON;
        this.testSuite = testSuite;
        this.runningType = runningType;
        this.appId = appId;
        this.deviceId = deviceId;
        this.success = success;
    }

    public PerformanceTestResultEntity() {
    }
}
