package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.BlockedDeviceInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockedDeviceInfoRepository extends JpaRepository<BlockedDeviceInfo, String>, JpaSpecificationExecutor<BlockedDeviceInfo> {
    BlockedDeviceInfo findByBlockedDeviceSerialNumber(String blockedDeviceSerialNumber);

    boolean existsByBlockedDeviceSerialNumber(String blockedDeviceSerialNumber);

    void deleteByBlockedDeviceSerialNumber(String blockedDeviceSerialNumber);
}
