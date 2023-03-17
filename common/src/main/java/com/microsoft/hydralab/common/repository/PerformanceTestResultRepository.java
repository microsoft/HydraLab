// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.PerformanceTestResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * @author taoran
 * @date 3/14/2023
 */

@Repository
public interface PerformanceTestResultRepository extends JpaRepository<PerformanceTestResultEntity, String>, JpaSpecificationExecutor<PerformanceTestResultEntity> {
    @Query(value = "select p from PerformanceTestResultEntity p " +
            "where p.testSuite = ?1 " +
            "and p.pkgName = ?2 " +
            "and p.runningType = ?3 " +
            "and p.parserType = ?4 " +
            "and p.date > ?5 " +
            "order by date asc")
    List<PerformanceTestResultEntity> findByTestSuiteAndPkgNameAndRunningTypeAndParserType(String testSuite, String pkgName, String runningType, String parserType,
                                                                                           Date beforeDate);
}
