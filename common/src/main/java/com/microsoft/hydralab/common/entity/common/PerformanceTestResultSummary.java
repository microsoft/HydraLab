// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.UUID;

/**
 * @author taoran
 * @date 3/2/2023
 */
@Data
@Entity
@Table(indexes = {@Index(columnList = "team_id")})
public class PerformanceTestResultSummary {
    @Id
    private String id = UUID.randomUUID().toString();
    private String testRunId;
    private String inspectorType;
    private String parserType;
    @Column(name = "team_id")
    private String teamId;
    private String rawJsonData;
    private long timestamp = System.currentTimeMillis();

    public PerformanceTestResultSummary(String testRunId) {
        this.testRunId = testRunId;
    }
}
