// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.center;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(name = "sys_permission")
public class SysPermission implements GrantedAuthority {
    @Id
    private String permissionId = UUID.randomUUID().toString();

    // URI / METHOD
    private String permissionType;

    // permission target
    @Column(name = "permission_content", nullable = false, unique = true)
    private String permissionContent;
    private Date createTime;
    private Date updateTime;

    @Override
    public String getAuthority() {
        return permissionContent;
    }
}
