// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AndroidTestUnitRepository extends JpaRepository<AndroidTestUnit, String> {
    List<AndroidTestUnit> findByDeviceTestResultId(String id);

    Page<AndroidTestUnit> findBySuccess(boolean success, Pageable pageable);
}
