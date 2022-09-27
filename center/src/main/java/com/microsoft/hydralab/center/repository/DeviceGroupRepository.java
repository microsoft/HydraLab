// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.common.entity.center.DeviceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DeviceGroupRepository extends JpaRepository<DeviceGroup, String>, JpaSpecificationExecutor<DeviceGroup> {
    List<DeviceGroup> queryAllByOwner(String owner);
    int countByGroupName(String groupName);
    List<DeviceGroup> findAllByTeamId(String teamId);
}