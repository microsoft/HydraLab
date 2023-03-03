// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.center;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

@Data
@Entity
@Table(indexes = {@Index(columnList = "team_id")})
public class DeviceGroup implements Serializable {
    @Id
    @Column(name = "group_name", nullable = false, length = 128)
    private String groupName;
    private String groupDisplayName;
    // userGroup/sysGroup
    private String groupType;
    private String owner;
    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate = false;
    @Transient
    private String serialNums;
    @Column(name = "team_id")
    private String teamId;
    private String teamName;
}