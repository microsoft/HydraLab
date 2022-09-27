// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.common.entity.center.StabilityData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StabilityDataRepository extends JpaRepository<StabilityData, String> {
    Page<StabilityData> findBySuccess(boolean success, Pageable pageable);
}