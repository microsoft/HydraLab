// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.repository;

import com.microsoft.hydralab.common.entity.agent.MobileDevice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileDeviceRepository extends JpaRepository<MobileDevice, String> {
    int countBySerialNum(String serialNum);
    MobileDevice getFirstBySerialNum(String serialNum);
}