package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.test.BaseTest;
import com.microsoft.hydralab.common.entity.center.SysRole;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.TaskResult;
import com.microsoft.hydralab.common.entity.common.scanner.ApkManifest;
import com.microsoft.hydralab.common.entity.common.scanner.ApkReport;
import com.microsoft.hydralab.common.repository.TaskResultRepository;
import com.microsoft.hydralab.common.util.Const;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.util.List;

public class SysUserServiceTest extends BaseTest {

    @Resource
    SysUserService sysUserService;
    @Resource
    SysRoleService sysRoleService;
    @Resource
    SysTeamService sysTeamService;
    @Resource
    UserTeamManagementService userTeamManagementService;

    @Resource
    TestDataService testDataService;

    @Resource
    TaskResultRepository taskResultRepository;

    @Test
    public void createUser() {
        SysRole defaultRole = sysRoleService.getOrCreateDefaultRole(Const.DefaultRole.USER, 100);
        SysTeam defaultTeam = sysTeamService.getOrCreateDefaultTeam(Const.DefaultTeam.DEFAULT_TEAM_NAME);
        SysUser user = sysUserService.createUserWithDefaultRole("test", "test@test.com", defaultRole.getRoleId(), defaultRole.getRoleName());

        Assertions.assertNotNull(user.getUserId(), "Create user Error!");
        user.setDefaultTeamId(defaultTeam.getTeamId());
        user.setDefaultTeamName(defaultTeam.getTeamName());
        userTeamManagementService.addUserTeamRelation(defaultTeam.getTeamId(), user, false);
        baseLogger.info("success");
    }

    @Test
    public void testSave(){
        TaskResult taskResult = new TaskResult();
        taskResult.addReportFile("test");
        taskResult.addReportFile("test2");
        taskResult.setState(TaskResult.TaskState.PASS.name());
        taskResultRepository.save(taskResult);

        ApkReport apkReport = new ApkReport();
        apkReport.addReportFile("atest");
        apkReport.addReportFile("atest2");
        apkReport.setState(TaskResult.TaskState.FAIL.name());
        ApkManifest apkmainfest = new ApkManifest();
        apkmainfest.setPackageName("testname");
        apkReport.setApkManifest(apkmainfest);
        taskResultRepository.save(apkReport);

    }

    @Test
    public void query(){
        List<TaskResult> results = taskResultRepository.findAll();
        System.out.println(results.size());
    }

}