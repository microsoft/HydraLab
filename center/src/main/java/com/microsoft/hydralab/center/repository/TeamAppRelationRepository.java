// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.common.entity.center.TeamAppRelation;
import com.microsoft.hydralab.common.entity.center.TeamAppRelationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamAppRelationRepository extends JpaRepository<TeamAppRelation, TeamAppRelationId> {
    Optional<TeamAppRelation> findByAppClientIdAndTeamId(String appClientId, String teamId);
}
