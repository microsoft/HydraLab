package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.TestJsonInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestJsonInfoRepository extends JpaRepository<TestJsonInfo, String>, JpaSpecificationExecutor<TestJsonInfo> {
    List<TestJsonInfo> findAllByTeamId(String teamId);
    List<TestJsonInfo> findByIsLatest(boolean isLatest);

    List<TestJsonInfo> findByIsLatestAndPackageNameAndCaseName(boolean isLatest, String packageName, String caseName);

    List<TestJsonInfo> findByPackageNameAndCaseName(String packageName, String caseName);
}
