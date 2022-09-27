// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.common.entity.center.RolePermissionRelation;
import com.microsoft.hydralab.common.entity.center.RolePermissionRelationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolePermissionRelationRepository extends JpaRepository<RolePermissionRelation, RolePermissionRelationId> {
    void deleteAllByPermissionId(String permissionId);
    void deleteAllByRoleId(String roleId);
    Optional<RolePermissionRelation> findByRoleIdAndPermissionId(String roleId, String permissionId);
}
