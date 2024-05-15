package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.StatisticData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatisticDataRepository extends JpaRepository<StatisticData, String> {
}
