package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.TaskResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskResultRepository extends JpaRepository<TaskResult, String> {
    Optional<TaskResult> findByTaskRunId(String taskRunId);
}