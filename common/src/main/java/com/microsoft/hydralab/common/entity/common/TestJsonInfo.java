// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@Entity
@Table(indexes = {@Index(columnList = "team_id")})
public class TestJsonInfo {
    public static final SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Id
    private long id;
    private String packageName;
    private String caseName;
    private String blobPath;
    private String blobUrl;
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
        return formatDate.format(ingestTime);
    }
}
