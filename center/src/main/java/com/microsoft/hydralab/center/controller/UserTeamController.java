// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.controller;

import com.microsoft.hydralab.center.service.*;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping
public class UserTeamController {
    @Resource
    SysTeamService sysTeamService;
    @Resource
    SysUserService sysUserService;
    @Resource
    UserTeamManagementService userTeamManagementService;
    @Resource
    SecurityUserService securityUserService;

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @PostMapping(value = {"/api/team/create"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<SysTeam> createTeam(@RequestParam("teamName") String teamName) {
        SysTeam sysTeam = sysTeamService.queryTeamByName(teamName);
        if (sysTeam != null) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "Team name exists.");
        }

        sysTeam = sysTeamService.createTeam(teamName);
        return Result.ok(sysTeam);
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @PostMapping(value = {"/api/team/delete"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result deleteTeam(@RequestParam("teamId") String teamId) {
        SysTeam sysTeam = sysTeamService.queryTeamById(teamId);
        if (sysTeam == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Team doesn't exist.");
        }
        if (userTeamManagementService.checkUserExistenceWithTeam(sysTeam.getTeamId())) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "There are still users under this TEAM, operation is forbidden.");
        }

        sysTeamService.deleteTeam(sysTeam);
        return Result.ok("Delete Team success!");
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN
     * 2) TEAM admin of the modified TEAM
     */
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','TEAM_ADMIN')")
    @PostMapping(value = {"/api/team/update"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<SysTeam> updateTeam(@CurrentSecurityContext SysUser requestor,
                                      @RequestParam("teamId") String teamId,
                                      @RequestParam(value = "teamName") String teamName) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        if (sysUserService.checkUserRole(requestor, Const.DefaultRole.TEAM_ADMIN) && !userTeamManagementService.checkRequestorTeamRelation(requestor, teamId)) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized for another team");
        }
        SysTeam sysTeam = sysTeamService.queryTeamById(teamId);
        if (sysTeam == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Team doesn't exist.");
        }
        SysTeam someTeam = sysTeamService.queryTeamByName(teamName);
        if (someTeam != null && !someTeam.getTeamId().equals(teamId)) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "Team name exists.");
        }
        if (sysTeam.getTeamName().equals(teamName)) {
            return Result.ok(sysTeam);
        }

        sysTeam.setTeamName(teamName);
        sysTeamService.updateTeam(sysTeam);
        return Result.ok(sysTeam);
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @GetMapping(value = {"/api/team/list"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<SysTeam>> getTeamList() {
        return Result.ok(sysTeamService.queryTeams());
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN
     * 2) TEAM admin of the modified TEAM
     */
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','TEAM_ADMIN')")
    @PostMapping(value = {"/api/userTeam/addRelation"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<UserTeamRelation> addUserTeamRelation(@CurrentSecurityContext SysUser requestor,
                                                        @RequestParam("userId") String userId,
                                                        @RequestParam("teamId") String teamId,
                                                        @RequestParam("isTeamAdmin") boolean isTeamAdmin) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        if (sysUserService.checkUserRole(requestor, Const.DefaultRole.TEAM_ADMIN) && !userTeamManagementService.checkRequestorTeamRelation(requestor, teamId)) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized for another team");
        }
        SysTeam team = sysTeamService.queryTeamById(teamId);
        if (team == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Team id is wrong.");
        }
        SysUser user = sysUserService.queryUserById(userId);
        if (user == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "User id is wrong.");
        }
        UserTeamRelation relation = userTeamManagementService.queryRelation(user.getMailAddress(), teamId);
        if (relation != null) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "Relation exists.");
        }

        relation = userTeamManagementService.addUserTeamRelation(teamId, user, isTeamAdmin);
        securityUserService.reloadUserAuthentication(user.getMailAddress(), Const.AUTH_COMPONENT.TEAM);
        return Result.ok(relation);
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN
     * 2) TEAM admin of the modified TEAM
     */
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','TEAM_ADMIN')")
    @PostMapping(value = {"/api/userTeam/deleteRelation"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result deleteUserTeamRelation(@CurrentSecurityContext SysUser requestor,
                                         @RequestParam("userId") String userId,
                                         @RequestParam("teamId") String teamId) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        if (sysUserService.checkUserRole(requestor, Const.DefaultRole.TEAM_ADMIN) && !userTeamManagementService.checkRequestorTeamRelation(requestor, teamId)) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized for another team");
        }
        SysTeam team = sysTeamService.queryTeamById(teamId);
        if (team == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Team id is wrong.");
        }
        SysUser user = sysUserService.queryUserById(userId);
        if (user == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "User id is wrong.");
        }
        UserTeamRelation relation = userTeamManagementService.queryRelation(user.getMailAddress(), teamId);
        if (relation == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Relation doesn't exist.");
        }

        userTeamManagementService.deleteUserTeamRelation(relation);
        securityUserService.reloadUserAuthentication(user.getMailAddress(), Const.AUTH_COMPONENT.TEAM);
        return Result.ok("delete user-team relation success!");
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN
     * 2) TEAM admin of the queried TEAM
     */
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','TEAM_ADMIN')")
    @PostMapping(value = {"/api/userTeam/queryUsers"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<SysUser>> queryUsersByTeam(@CurrentSecurityContext SysUser requestor,
                                                  @RequestParam("teamId") String teamId) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        if (sysUserService.checkUserRole(requestor, Const.DefaultRole.TEAM_ADMIN) && !userTeamManagementService.checkRequestorTeamRelation(requestor, teamId)) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized for another team");
        }

        List<SysUser> userList = userTeamManagementService.queryUsersByTeam(teamId);
        return Result.ok(userList);
    }

    /**
     * Authenticated USER: all
     */
    @GetMapping(value = {"/api/userTeam/listSelfTeam"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<SysTeam>> getSelfTeamList(@CurrentSecurityContext SysUser requestor) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }

        return Result.ok(userTeamManagementService.queryTeamsByUser(requestor.getMailAddress()));
    }
}
