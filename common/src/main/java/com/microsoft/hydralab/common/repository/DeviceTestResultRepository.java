// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTestResultRepository extends JpaRepository<DeviceTestTask, String> {

    List<DeviceTestTask> findByTestTaskId(String taskId);
    Optional<DeviceTestTask> findByCrashStackId(String crashStackId);
}
