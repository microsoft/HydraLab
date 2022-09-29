// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.center;

import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.*;
import java.util.*;

@Data
@Entity
@Table(name = "sys_user")
public class SysUser implements Authentication {
    private static final long serialVersionUID = 1L;

    @Id
    private String userId = UUID.randomUUID().toString();

    @Column(name = "user_name", nullable = false)
    private String userName;
    @Column(name = "mail_address", nullable = false, unique = true)
    private String mailAddress;
    @Column(name = "role_id")
    private String roleId;
    @Column(name = "default_team_id")
    private String defaultTeamId;

    // store TEAM info that the USER is in, k-v: teamId -> isTeamAdmin
    @Transient
    private transient Map<String, Boolean> teamAdminMap = new HashMap<>();
    @Transient
    private transient List<GrantedAuthority> authorities = new ArrayList<>();
    @Transient
    private transient String accessToken;

    public void setAuthorities(List<GrantedAuthority> permissions) {
        this.authorities = permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return mailAddress;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

    }

    @Override
    public String getName() {
        return userName;
    }
}
