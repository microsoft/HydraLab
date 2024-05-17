// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.repository.RolePermissionRelationRepository;
import com.microsoft.hydralab.center.repository.SysRoleRepository;
import com.microsoft.hydralab.common.entity.center.SysRole;
import com.microsoft.hydralab.common.entity.center.SysUser;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SysRoleService {
    //save Role list <roleId, SysRole>
    private final Map<String, SysRole> roleListMap = new ConcurrentHashMap<>();
    @Resource
    SysRoleRepository sysRoleRepository;
    @Resource
    RolePermissionRelationRepository rolePermissionRelationRepository;

    @PostConstruct
    public void initList() {
        List<SysRole> roleList = sysRoleRepository.findAll();
        roleList.forEach(role -> roleListMap.put(role.getRoleId(), role));
    }

    public SysRole createRole(String roleName, int authLevel) {
        SysRole sysRole = new SysRole();
        sysRole.setRoleName(roleName);
        sysRole.setCreateTime(new Date());
        sysRole.setUpdateTime(new Date());
        sysRole.setAuthLevel(authLevel);

        roleListMap.put(sysRole.getRoleId(), sysRole);
        return sysRoleRepository.save(sysRole);
    }

    public SysRole updateRole(SysRole sysRole) {
        if (sysRole == null) {
            return null;
        }

        sysRole.setUpdateTime(new Date());
        roleListMap.put(sysRole.getRoleId(), sysRole);
        return sysRoleRepository.save(sysRole);
    }

    public SysRole queryRoleById(String roleId) {
        SysRole role = roleListMap.get(roleId);
        if (role != null) {
            return role;
        }

        Optional<SysRole> someRole = sysRoleRepository.findById(roleId);
        if (someRole.isPresent()) {
            role = someRole.get();
            roleListMap.put(role.getRoleId(), role);
        }

        return role;
    }

    public SysRole queryRoleByName(String roleName) {
        for (SysRole role : new ArrayList<>(roleListMap.values())) {
            if (role.getRoleName().equals(roleName)) {
                return role;
            }
        }

        Optional<SysRole> someRole = sysRoleRepository.findByRoleName(roleName);
        SysRole role = null;
        if (someRole.isPresent()) {
            role = someRole.get();
            roleListMap.put(role.getRoleId(), role);
        }

        return role;
    }

    public List<SysRole> queryRoles() {
        return new ArrayList<>(roleListMap.values());
    }

    public void deleteRole(SysRole role) {
        roleListMap.remove(role.getRoleId());
        sysRoleRepository.deleteById(role.getRoleId());
        rolePermissionRelationRepository.deleteAllByRoleId(role.getRoleId());
    }

    public boolean isAuthLevelValid(int authLevel) {
        // super admin (level 0) cannot be added
        return authLevel > 0;
    }

    public boolean isAuthLevelSuperior(SysRole aRole, SysRole bRole) {
        return aRole.getAuthLevel() < bRole.getAuthLevel();
    }

    public SysRole getRequestorRole(SysUser requestor) {
        if (requestor == null) {
            return null;
        }

        return queryRoleById(requestor.getRoleId());
    }

    public SysRole getOrCreateDefaultRole(String roleName, int authLevel) {
        SysRole role = queryRoleByName(roleName);
        if (role == null) {
            role = createRole(roleName, authLevel);
        }

        return role;
    }
}
