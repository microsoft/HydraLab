// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.TestTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestTaskRepository extends JpaRepository<TestTask, String>, JpaSpecificationExecutor<TestTask> {
    List<TestTask> findAllByTeamId(String teamId);
}
