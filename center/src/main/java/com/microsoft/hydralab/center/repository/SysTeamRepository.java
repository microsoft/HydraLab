// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.common.entity.center.SysTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SysTeamRepository extends JpaRepository<SysTeam, String> {
    Optional<SysTeam> findByTeamName(String teamName);
}
