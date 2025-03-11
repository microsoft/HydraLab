package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.BlockedDeviceInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.Instant;
import java.util.List;

@Repository
public interface BlockedDeviceInfoRepository extends JpaRepository<BlockedDeviceInfo, String>, JpaSpecificationExecutor<BlockedDeviceInfo> {
    @Transactional
    BlockedDeviceInfo findByBlockedDeviceSerialNumber(String blockedDeviceSerialNumber);

    @Transactional
    boolean existsByBlockedDeviceSerialNumber(String blockedDeviceSerialNumber);

    @Modifying
    @Transactional
    void deleteByBlockedDeviceSerialNumber(String blockedDeviceSerialNumber);

    @Modifying
    @Transactional
    @Query("DELETE FROM BlockedDeviceInfo b WHERE b.blockedTime < :time")
    void deleteByBlockedTimeBefore(@Param("time") Instant time);
}
