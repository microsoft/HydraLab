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
import java.util.Optional;

@Repository
public interface BlockedDeviceInfoRepository extends JpaRepository<BlockedDeviceInfo, String>, JpaSpecificationExecutor<BlockedDeviceInfo> {
    @Query("SELECT b FROM BlockedDeviceInfo b WHERE b.blockedDeviceSerialNumber = :serialNumber")
    Optional<BlockedDeviceInfo> findByBlockedDeviceSerialNumber(@Param("serialNumber") String serialNumber);

    @Modifying
    @Transactional
    @Query("DELETE FROM BlockedDeviceInfo b WHERE b.blockedTime < :time")
    void deleteByBlockedTimeBefore(@Param("time") Instant time);

    @Modifying
    @Transactional
    @Query("DELETE FROM BlockedDeviceInfo b WHERE b.blockedDeviceSerialNumber = :serialNumber")
    void deleteIfExists(@Param("serialNumber") String serialNumber);

}
