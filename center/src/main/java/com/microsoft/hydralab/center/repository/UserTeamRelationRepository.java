// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.common.entity.center.UserTeamRelation;
import com.microsoft.hydralab.common.entity.center.UserTeamRelationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserTeamRelationRepository extends JpaRepository<UserTeamRelation, UserTeamRelationId> {
    Optional<UserTeamRelation> findByMailAddressAndTeamId(String mailAddress, String teamId);
    List<UserTeamRelation> findAllByMailAddressAndIsTeamAdmin(String mailAddress, boolean isTeamAdmin);
    List<UserTeamRelation> findAllByMailAddress(String mailAddress);
    List<UserTeamRelation> findAllByTeamId(String teamId);
    void deleteAllByTeamId(String roleId);
}
