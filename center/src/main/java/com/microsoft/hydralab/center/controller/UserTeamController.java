// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.controller;

import com.microsoft.hydralab.center.service.*;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

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
    public Result deleteTeam(@CurrentSecurityContext SysUser requestor, @RequestParam("teamId") String teamId) {
        SysTeam sysTeam = sysTeamService.queryTeamById(teamId);
        if (sysTeam == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Team doesn't exist.");
        }
        if (Const.DefaultTeam.DEFAULT_TEAM_NAME.equals(sysTeam.getTeamName())) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "Cannot delete default team as it's for public use.");
        }
        // if the only user in TEAM is the requestor ADMIN & TEAM_ADMIN, make it deletable
        if (userTeamManagementService.checkUserExistenceWithTeam(requestor, sysTeam.getTeamId())) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "There are still users under this team, operation is forbidden.");
        }

        sysTeamService.deleteTeam(sysTeam);
        return Result.ok("Delete team success!");
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
    @GetMapping(value = {"/api/userTeam/listAuthorizedTeam"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<SysTeam>> getAuthorizedTeam(@CurrentSecurityContext SysUser requestor) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }

        List<SysTeam> teamList;
        if (sysUserService.checkUserAdmin(requestor)) {
            teamList = sysTeamService.queryTeams();
            teamList.forEach(team -> team.setManageable(true));
        } else {
            teamList = userTeamManagementService.queryTeamsByUser(requestor.getMailAddress());
            if (sysUserService.checkUserRole(requestor, Const.DefaultRole.TEAM_ADMIN)) {
                requestor.getTeamAdminMap().entrySet().stream().filter(Map.Entry::getValue).forEach(
                        isTeamAdmin -> {
                            String adminTeamId = isTeamAdmin.getKey();
                            for (SysTeam team : teamList) {
                                if (team.getTeamId().equals(adminTeamId)) {
                                    team.setManageable(true);
                                    break;
                                }
                            }
                        }
                );
            }
        }

        return Result.ok(teamList);
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

    /**
     * Authenticated USER:
     * 1) USERs with ROLE SUPER_ADMIN/ADMIN can switch all USER's default TEAM info
     * 2) USERs can switch their own default TEAM info
     */
    @PostMapping(value = {"/api/userTeam/switchDefaultTeam"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<SysUser> switchDefaultTeam(@CurrentSecurityContext SysUser requestor,
                                             @RequestParam(value = "mailAddress", required = false) String mailAddress,
                                             @RequestParam("teamId") String teamId) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
        }

        SysTeam team = sysTeamService.queryTeamById(teamId);
        if (team == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "TEAM id is wrong.");
        }
        SysUser user;
        if (StringUtils.isEmpty(mailAddress)) {
            // [All USERs] request for self default TEAM update
            mailAddress = requestor.getMailAddress();
            user = requestor;
        } else {
            // [Admin only] request for others' default TEAM update
            user = sysUserService.queryUserByMailAddress(mailAddress);
            if (user == null) {
                return Result.error(HttpStatus.BAD_REQUEST.value(), "USER id is wrong.");
            }
        }

        if (sysUserService.checkUserAdmin(requestor) || mailAddress.equals(requestor.getMailAddress())) {
            if (!userTeamManagementService.checkUserTeamRelation(mailAddress, teamId)) {
                return Result.error(HttpStatus.BAD_REQUEST.value(), "USER isn't under the TEAM, cannot switch the default TEAM to it.");
            }
        } else {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized to operate on this USER");
        }
        sysUserService.switchUserDefaultTeam(user, teamId, team.getTeamName());
        securityUserService.reloadUserAuthentication(user.getMailAddress(), Const.AUTH_COMPONENT.DEFAULT_TEAM);
        return Result.ok(user);
    }

    /**
     * Authenticated USER:
     * 1) USERs with ROLE SUPER_ADMIN/ADMIN can query all USER's default TEAM info
     * 2) USERs can query their own default TEAM info
     */
    @PostMapping(value = {"/api/userTeam/queryDefaultTeam"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<SysTeam> queryDefaultTeam(@CurrentSecurityContext SysUser requestor,
                                            @RequestParam(value = "mailAddress", required = false) String mailAddress) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
        }

        SysUser user;
        if (StringUtils.isEmpty(mailAddress)) {
            // [All USERs] request for self default TEAM update
            user = requestor;
        } else {
            // [Admin only] request for others' default TEAM update
            if (!sysUserService.checkUserAdmin(requestor)) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized to query info of this USER");
            }

            user = sysUserService.queryUserByMailAddress(mailAddress);
            if (user == null) {
                return Result.error(HttpStatus.BAD_REQUEST.value(), "USER id is wrong.");
            }
        }

        return Result.ok(sysTeamService.queryTeamById(user.getDefaultTeamId()));
    }

}
