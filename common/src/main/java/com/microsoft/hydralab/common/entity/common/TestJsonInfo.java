// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@Entity
@Table(indexes = {@Index(columnList = "team_id")})
public class TestJsonInfo {
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Id
    private long id;
    private String packageName;
    private String caseName;
    private String blobPath;
    private String blobUrl;
    private String blobContainer;
    private boolean isLatest;
    @Column(name = "team_id")
    private String teamId;
    private String teamName;

    private Date ingestTime;

    public TestJsonInfo() {
        this.id = System.nanoTime();
        this.ingestTime = new Date();
    }

    public String getDisplayIngestTime() {
        return SIMPLE_DATE_FORMAT.format(ingestTime);
    }
}
