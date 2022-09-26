// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.common.entity.center.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, String> {
    Optional<SysUser> findByMailAddress(String mailAddress);
    int countByRoleId(String roleId);
}
