// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.common.entity.center.DeviceGroupRelation;
import com.microsoft.hydralab.common.entity.center.DeviceGroupRelationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import javax.transaction.Transactional;
import java.util.List;

public interface DeviceGroupRelationRepository extends JpaRepository<DeviceGroupRelation, DeviceGroupRelationId> {
    List<DeviceGroupRelation> findAllByDeviceSerial(String deviceSerial);
    List<DeviceGroupRelation> findAllByGroupName(String groupName);
    @Modifying
    @Transactional
    void deleteAllByGroupName(String groupName);
}