// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.KeyValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeyValueDBRepository extends JpaRepository<KeyValue, String> {

}
