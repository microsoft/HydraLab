// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.center;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(name = "sys_team")
public class SysTeam {
    @Id
    private String teamId = UUID.randomUUID().toString();

    @Column(name = "team_name", nullable = false, unique = true)
    private String teamName;
    private Date createTime;
    private Date updateTime;
    @Transient
    // if the team is manageable by API requestor
    private boolean manageable = false;
}
