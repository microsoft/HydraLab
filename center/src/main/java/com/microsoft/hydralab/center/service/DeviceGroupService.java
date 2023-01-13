// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.service;


import com.microsoft.hydralab.common.entity.center.DeviceGroupRelationId;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.entity.center.DeviceGroup;
import com.microsoft.hydralab.common.entity.center.DeviceGroupRelation;
import com.microsoft.hydralab.center.repository.DeviceGroupRelationRepository;
import com.microsoft.hydralab.center.repository.DeviceGroupRepository;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.util.CriteriaTypeUtil;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@CacheConfig(cacheNames = "groupCache")
public class DeviceGroupService {
    @Resource
    DeviceGroupRepository deviceGroupRepository;
    @Resource
    DeviceGroupRelationRepository deviceGroupRelationRepository;
    @Resource
    SysUserService sysUserService;
    @Resource
    UserTeamManagementService userTeamManagementService;


    public DeviceGroup createGroup(String teamId, String teamName, String groupName, String owner) {
        DeviceGroup deviceGroup = new DeviceGroup();
        deviceGroup.setTeamId(teamId);
        deviceGroup.setTeamName(teamName);
        deviceGroup.setGroupName(Const.DeviceGroup.GROUP_NAME_PREFIX + groupName);
        deviceGroup.setGroupDisplayName(groupName);
        deviceGroup.setGroupType(Const.DeviceGroup.USER_GROUP);
        return deviceGroupRepository.save(deviceGroup);
    }

    public DeviceGroup updateGroup(DeviceGroup deviceGroup) {
        return deviceGroupRepository.save(deviceGroup);
    }

    public DeviceGroup getGroupByName(String groupName) {
        return deviceGroupRepository.findById(groupName).orElse(null);
    }

    public void deleteGroup(String groupName) {
        deviceGroupRepository.deleteById(groupName);
        deviceGroupRelationRepository.deleteAllByGroupName(groupName);
    }

    public List<DeviceGroup> queryAllGroups() {
        return deviceGroupRepository.findAll();
    }

    public List<DeviceGroup> queryFilteredGroups(List<CriteriaType> queryParams) {
        Specification<DeviceGroup> spec = null;
        if (queryParams != null && queryParams.size() > 0) {
            spec = new CriteriaTypeUtil<DeviceGroup>().transferToSpecification(queryParams, true);
        }

        return deviceGroupRepository.findAll(spec);
    }

    public List<DeviceGroup> queryGroupsByOwner(String owner) {
        return deviceGroupRepository.queryAllByOwner(owner);
    }

    private List<DeviceGroup> getGroupsByTeamId(String teamId) {
        return deviceGroupRepository.findAllByTeamId(teamId);
    }

    @CacheEvict(key = "'groups-'+#deviceSerial")
    public DeviceGroupRelation saveRelation(String groupName, String deviceSerial) {
        DeviceGroupRelation deviceGroupRelation = new DeviceGroupRelation(groupName, deviceSerial);
        return deviceGroupRelationRepository.save(deviceGroupRelation);
    }

    @CacheEvict(key = "'groups-'+#deviceSerial")
    public void deleteRelation(String groupName, String deviceSerial) {
        DeviceGroupRelation deviceGroupRelation = new DeviceGroupRelation(groupName, deviceSerial);
        deviceGroupRelationRepository.delete(deviceGroupRelation);
    }

    public DeviceGroupRelation getRelation(String groupName, String deviceSerial) {
        DeviceGroupRelationId id = new DeviceGroupRelationId();
        id.setDeviceSerial(deviceSerial);
        id.setGroupName(groupName);
        return deviceGroupRelationRepository.findById(id).orElse(null);
    }

    public List<DeviceGroupRelation> getDeviceByGroup(String groupName) {
        return deviceGroupRelationRepository.findAllByGroupName(groupName);
    }

    @Cacheable(key = "'groups-'+#deviceSerial")
    public List<DeviceGroupRelation> getGroupByDevice(String deviceSerial) {
        return deviceGroupRelationRepository.findAllByDeviceSerial(deviceSerial);
    }

    public boolean checkGroupAuthorization(SysUser requestor, String groupName, boolean teamAdminRequired) throws IllegalArgumentException{
        if (requestor == null) {
            return false;
        }

        if (groupName == null || "".equals(groupName)) {
            throw new IllegalArgumentException("groupName is required");
        }
        DeviceGroup deviceGroup = getGroupByName(groupName);
        if (deviceGroup == null) {
            throw new IllegalArgumentException("groupName is incorrect");
        }

        // ROLE = SUPER_ADMIN / ADMIN
        if (sysUserService.checkUserAdmin(requestor)) {
            return true;
        }

        if (teamAdminRequired) {
            // TEAM_ADMIN of current TEAM
            return userTeamManagementService.checkRequestorTeamAdmin(requestor, deviceGroup.getTeamId());
        }
        else {
            return userTeamManagementService.checkRequestorTeamRelation(requestor, deviceGroup.getTeamId());
        }
    }

    public boolean checkGroupName(String groupName) {
        int i = deviceGroupRepository.countByGroupName(groupName);
        if (i == 0) {
            return false;
        }
        return true;
    }

    public boolean isGroupNameIllegal(String groupName) {
        Pattern p = Pattern.compile("[^A-Za-z0-9]");
        Matcher m = p.matcher(groupName);
        return m.find();
    }

    public void updateGroupTeam(String teamId, String teamName) {
        List<DeviceGroup> deviceGroups = getGroupsByTeamId(teamId);
        deviceGroups.forEach(group -> group.setTeamName(teamName));

        deviceGroupRepository.saveAll(deviceGroups);
    }
}
