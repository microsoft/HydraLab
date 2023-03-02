// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.repository.RolePermissionRelationRepository;
import com.microsoft.hydralab.center.repository.SysPermissionRepository;
import com.microsoft.hydralab.common.entity.center.SysPermission;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class SysPermissionService {
    //save Permission list <permissionId, SysPermission>
    private final Map<String, SysPermission> permissionListMap = new ConcurrentHashMap<>();
    @Resource
    SysPermissionRepository sysPermissionRepository;
    @Resource
    RolePermissionRelationRepository permissionPermissionRelationRepository;

    @PostConstruct
    public void initList() {
        List<SysPermission> permissionList = sysPermissionRepository.findAll();
        permissionList.forEach(permission -> permissionListMap.put(permission.getPermissionId(), permission));
    }

    public SysPermission createPermission(String permissionType, String permissionContent) {
        SysPermission sysPermission = new SysPermission();

        if (!permissionType.isEmpty()) {
            sysPermission.setPermissionType(permissionType);
        }
        sysPermission.setPermissionContent(permissionContent);
        sysPermission.setCreateTime(new Date());
        sysPermission.setUpdateTime(new Date());

        permissionListMap.put(sysPermission.getPermissionId(), sysPermission);
        return sysPermissionRepository.save(sysPermission);
    }

    public SysPermission updatePermission(SysPermission sysPermission) {
        if (sysPermission == null) {
            return null;
        }

        sysPermission.setUpdateTime(new Date());
        permissionListMap.put(sysPermission.getPermissionId(), sysPermission);
        return sysPermissionRepository.save(sysPermission);
    }

    public SysPermission queryPermissionById(String permissionId) {
        SysPermission permission = permissionListMap.get(permissionId);
        if (permission != null) {
            return permission;
        }

        Optional<SysPermission> somePermission = sysPermissionRepository.findById(permissionId);
        if (somePermission.isPresent()) {
            permission = somePermission.get();
            permissionListMap.put(permission.getPermissionId(), permission);
        }

        return permission;
    }

    public SysPermission queryPermissionByContent(String permissionContent) {
        for (SysPermission permission : new ArrayList<>(permissionListMap.values())) {
            if (permission.getPermissionContent().equals(permissionContent)) {
                return permission;
            }
        }

        Optional<SysPermission> somePermission = sysPermissionRepository.findByPermissionContent(permissionContent);
        SysPermission permission = null;
        if (somePermission.isPresent()) {
            permission = somePermission.get();
            permissionListMap.put(permission.getPermissionId(), permission);
        }

        return permission;
    }

    public List<SysPermission> queryPermissionsByType(String type) {
        List<SysPermission> permissions = sysPermissionRepository.findAllByPermissionType(type);
        permissions.forEach(permission -> permissionListMap.put(permission.getPermissionId(), permission));

        return permissions;
    }

    public List<SysPermission> queryPermissions() {
        return new ArrayList<>(permissionListMap.values());
    }

    public void deletePermission(SysPermission permission) {
        permissionListMap.remove(permission.getPermissionId());
        sysPermissionRepository.deleteById(permission.getPermissionId());
        permissionPermissionRelationRepository.deleteAllByPermissionId(permission.getPermissionId());
    }
}
