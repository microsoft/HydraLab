// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.common.entity.center.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    List<AuthToken> queryByCreator(String creator);

    int countByToken(String token);

    List<AuthToken> queryByToken(String token);
}