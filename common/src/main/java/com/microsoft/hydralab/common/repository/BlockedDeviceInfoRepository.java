package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.BlockedDeviceInfo;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedDeviceInfoRepository extends JpaRepository<BlockedDeviceInfo, String>, JpaSpecificationExecutor<BlockedDeviceInfo> {
    @Transactional
    Optional<BlockedDeviceInfo> findByBlockedDeviceSerialNumber(@Param("serialNumber") String serialNumber);

    @Modifying
    @Transactional
    @Query("DELETE FROM BlockedDeviceInfo b WHERE b.blockedTime < :blockedTime")
    void deleteByBlockedTimeBefore(@Param("blockedTime") Instant blockedTime);

    @Modifying
    @Transactional
    @Query("DELETE FROM BlockedDeviceInfo b WHERE b.blockedDeviceSerialNumber = :blockedDeviceSerialNumber")
    void deleteIfExists(@Param("blockedDeviceSerialNumber") String blockedDeviceSerialNumber);
}
