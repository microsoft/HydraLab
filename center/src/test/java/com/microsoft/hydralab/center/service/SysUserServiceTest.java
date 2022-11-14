package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.test.BaseTest;
import com.microsoft.hydralab.common.entity.center.SysRole;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.util.Const;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;

public class SysUserServiceTest extends BaseTest {

    @Resource
    SysUserService sysUserService;
    @Resource
    SysRoleService sysRoleService;
    @Resource
    SysTeamService sysTeamService;
    @Resource
    UserTeamManagementService userTeamManagementService;

    @Test
    public void createUser() {
        SysRole defaultRole = sysRoleService.getOrCreateDefaultRole(Const.DefaultRole.USER, 100);
        SysTeam defaultTeam = sysTeamService.getOrCreateDefaultTeam(Const.DefaultTeam.DEFAULT_TEAM_NAME);
        SysUser user = sysUserService.createUserWithDefaultRole("test", "test@test.com", defaultRole.getRoleId(), defaultRole.getRoleName());

        Assertions.assertNotNull(user.getUserId(), "Create user Error!");
        Assertions.assertNull(user.getUserId(), "Create user Error!");
        user.setDefaultTeamId(defaultTeam.getTeamId());
        user.setDefaultTeamName(defaultTeam.getTeamName());
        userTeamManagementService.addUserTeamRelation(defaultTeam.getTeamId(), user, false);
        baseLogger.info("success");
    }


}