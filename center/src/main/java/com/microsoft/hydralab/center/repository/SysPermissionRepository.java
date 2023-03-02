// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.common.entity.center.SysPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysPermissionRepository extends JpaRepository<SysPermission, String> {
    Optional<SysPermission> findByPermissionContent(String permissionContent);

    List<SysPermission> findAllByPermissionType(String type);
}
