// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.repository.SysUserRepository;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.entity.center.SysUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class SysUserService {
    // not storing user data in memory
    @Resource
    SysUserRepository sysUserRepository;

    public SysUser createUserWithDefaultRole(String userName, String mailAddress, String defaultRoleId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserName(userName);
        sysUser.setMailAddress(mailAddress);
        // default to be ROLE: user
        sysUser.setRoleId(defaultRoleId);

        return sysUserRepository.save(sysUser);
    }

    public SysUser updateUser(SysUser sysUser) {
        if (sysUser == null) {
            return null;
        }
        return sysUserRepository.save(sysUser);
    }

    public SysUser queryUserById(String userId) {
        return sysUserRepository.findById(userId).orElse(null);
    }

    public SysUser queryUserByMailAddress(String mailAddress) {
        return sysUserRepository.findByMailAddress(mailAddress).orElse(null);
    }

    public boolean checkUserExistenceWithRole(String roleId) {
        int count = sysUserRepository.countByRoleId(roleId);
        return count > 0;
    }

    public List<SysUser> queryUsers() {
        return sysUserRepository.findAll();
    }

    public SysUser switchUserRole(SysUser user, String roleId) {
        if (user.getRoleId().equals(roleId)) {
            return user;
        }

        user.setRoleId(roleId);
        return updateUser(user);
    }

    public boolean checkUserRole(Authentication auth, String roleName) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (authority.getAuthority().equals(roleName)) {
                return true;
            }
        }

        return false;
    }

    public boolean checkUserAdmin(Authentication auth) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (authority.getAuthority().equals(Const.DefaultRole.SUPER_ADMIN) || authority.getAuthority().equals(Const.DefaultRole.SUPER_ADMIN)) {
                return true;
            }
        }

        return false;
    }
}
