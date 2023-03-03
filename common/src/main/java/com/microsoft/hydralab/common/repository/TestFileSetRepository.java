// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.TestFileSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TestFileSetRepository extends JpaRepository<TestFileSet, String>, JpaSpecificationExecutor<TestFileSet> {
    List<TestFileSet> findAllByTeamId(String teamId);
}