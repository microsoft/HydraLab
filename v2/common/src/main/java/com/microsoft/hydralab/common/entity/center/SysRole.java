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
@Table(name = "sys_role")
public class SysRole implements GrantedAuthority {
    @Id
    private String roleId = UUID.randomUUID().toString();

    @Column(name = "role_name", nullable = false, unique = true)
    private String roleName;
    @Column(name = "auth_level", nullable = false)
    private int authLevel;
    private Date createTime;
    private Date updateTime;

    @Override
    public String getAuthority() {
        return roleName;
    }
}
