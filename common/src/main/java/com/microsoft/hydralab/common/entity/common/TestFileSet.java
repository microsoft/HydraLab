// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "test_file_set", indexes = {@Index(columnList = "team_id")})
@Data
public class TestFileSet {
    @Id
    private String id;
    private String buildType;
    private String runningType;
    private String appName;
    private String packageName;
    private String version;
    private String commitId;
    private String commitMessage;
    private String commitCount;
    private Date ingestTime;
    @Transient
    private List<BlobFileInfo> attachments = new ArrayList<>();
    @Column(name = "team_id")
    private String teamId;
    private String teamName;

    public TestFileSet() {
        id = UUID.randomUUID().toString();
        ingestTime = new Date();
    }
}