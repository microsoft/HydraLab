// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.PerformanceTestResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * @author taoran
 * @date 3/14/2023
 */

@Repository
public interface PerformanceTestResultRepository extends JpaRepository<PerformanceTestResultEntity, String>, JpaSpecificationExecutor<PerformanceTestResultEntity> {
}
