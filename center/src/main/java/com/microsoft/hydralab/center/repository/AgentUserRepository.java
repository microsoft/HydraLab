// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.common.entity.common.AgentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentUserRepository extends JpaRepository<AgentUser, String>, JpaSpecificationExecutor<AgentUser> {
    Page<AgentUser> findByStatus(String status, Pageable pageable);

    Optional<AgentUser> findByMailAddress(String mailAddress);

    Optional<AgentUser> findByName(String name);

    List<AgentUser> findAllByMailAddress(String mailAddress);

    List<AgentUser> findAllByTeamId(String teamId);
}
