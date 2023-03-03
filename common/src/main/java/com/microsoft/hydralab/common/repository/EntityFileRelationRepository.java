// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.EntityFileRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntityFileRelationRepository extends JpaRepository<EntityFileRelation, String> {
    List<EntityFileRelation> queryAllByEntityIdAndAndEntityType(String entityId, String entityType);
}