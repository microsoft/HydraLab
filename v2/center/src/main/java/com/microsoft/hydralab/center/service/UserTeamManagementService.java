// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.alibaba.fastjson.JSONArray;
import com.microsoft.hydralab.center.repository.UserTeamRelationRepository;
import com.microsoft.hydralab.common.entity.center.SysRole;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.center.UserTeamRelation;
import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.util.Const;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserTeamManagementService {
    //save USER & TEAM relation <mailAddress, SysTeam>
    private final Map<String, Set<SysTeam>> userTeamListMap = new ConcurrentHashMap<>();
    //save USER & TEAM relation <teamId, SysUser>
    private final Map<String, Set<SysUser>> teamUserListMap = new ConcurrentHashMap<>();
    //save TEAM ADMIN of all TEAMs <teamId, teamAdminMailAddress>
    private final Map<String, Set<String>> teamAdminListMap = new ConcurrentHashMap<>();

    @Resource
    UserTeamRelationRepository userTeamRelationRepository;
    @Resource
    SysTeamService sysTeamService;
    @Resource
    SysUserService sysUserService;
    @Resource
    SysRoleService sysRoleService;

    @PostConstruct
    public void initList() {
        List<UserTeamRelation> relationList = userTeamRelationRepository.findAll();
        relationList.forEach(relation -> {
            Set<SysTeam> teamList = userTeamListMap.computeIfAbsent(relation.getMailAddress(), k -> new HashSet<>());
            SysTeam team = sysTeamService.queryTeamById(relation.getTeamId());
            if (team != null) {
                teamList.add(team);
            }
            Set<SysUser> userList = teamUserListMap.computeIfAbsent(relation.getTeamId(), k -> new HashSet<>());
            SysUser user = sysUserService.queryUserByMailAddress(relation.getMailAddress());
            if (user != null) {
                userList.add(user);
            }
            if (relation.isTeamAdmin()) {
                Set<String> teamAdmins = teamAdminListMap.computeIfAbsent(relation.getTeamId(), k -> new HashSet<>());
                teamAdmins.add(relation.getMailAddress());
            }
        });
    }

    public UserTeamRelation addUserTeamRelation(String teamId, SysUser user, boolean isTeamAdmin) {
        Set<SysTeam> teamList = userTeamListMap.computeIfAbsent(user.getMailAddress(), k -> new HashSet<>());
        teamList.add(sysTeamService.queryTeamById(teamId));
        Set<SysUser> userList = teamUserListMap.computeIfAbsent(teamId, k -> new HashSet<>());
        userList.add(user);
        if (isTeamAdmin) {
            Set<String> teamAdmins = teamAdminListMap.computeIfAbsent(teamId, k -> new HashSet<>());
            teamAdmins.add(user.getMailAddress());

            SysRole originalRole = sysRoleService.queryRoleById(user.getRoleId());
            SysRole teamAdminRole = sysRoleService.getOrCreateDefaultRole(Const.DefaultRole.TEAM_ADMIN, 10);
            if (sysRoleService.isAuthLevelSuperior(teamAdminRole, originalRole)) {
                user.setRoleId(teamAdminRole.getRoleId());
                user.setRoleName(teamAdminRole.getRoleName());
                sysUserService.updateUser(user);
            }
        }

        UserTeamRelation userTeamRelation = new UserTeamRelation(teamId, user.getMailAddress(), isTeamAdmin);
        return userTeamRelationRepository.save(userTeamRelation);
    }

    public void deleteUserTeamRelation(UserTeamRelation relation) {
        Set<SysTeam> teamList = userTeamListMap.get(relation.getMailAddress());
        Set<SysUser> userList = teamUserListMap.get(relation.getTeamId());
        if (teamList == null && userList == null) {
            return;
        }
        if (teamList != null) {
            teamList.removeIf(team -> team.getTeamId().equals(relation.getTeamId()));
        }
        if (userList != null) {
            userList.removeIf(user -> user.getMailAddress().equals(relation.getMailAddress()));
        }

        if (relation.isTeamAdmin()) {
            Set<String> teamAdmins = teamAdminListMap.get(relation.getTeamId());
            if (teamAdmins != null) {
                teamAdmins.remove(relation.getMailAddress());
            }
        }

        userTeamRelationRepository.delete(relation);
    }

    public UserTeamRelation queryRelation(String mailAddress, String teamId) {
        return userTeamRelationRepository.findByMailAddressAndTeamId(mailAddress, teamId).orElse(null);
    }

    public List<UserTeamRelation> queryTeamRelations(String mailAddress, boolean isTeamAdmin) {
        return userTeamRelationRepository.findAllByMailAddressAndIsTeamAdmin(mailAddress, isTeamAdmin);
    }

    public List<UserTeamRelation> queryTeamRelationsByMailAddress(String mailAddress) {
        return userTeamRelationRepository.findAllByMailAddress(mailAddress);
    }

    public List<UserTeamRelation> queryTeamRelationsByTeamId(String teamId) {
        return userTeamRelationRepository.findAllByTeamId(teamId);
    }

    public List<SysUser> queryUsersByTeam(String teamId) {
        Set<SysUser> users = teamUserListMap.get(teamId);
        if (users == null) {
            return null;
        }

        return new ArrayList<>(users);
    }

    public List<SysUser> queryTeamUsersWithTeamAdmin(String teamId) {
        List<SysUser> allUsers = queryUsersByTeam(teamId);
        Set<String> teamAdminSet = teamAdminListMap.get(teamId);
        if (!CollectionUtils.isEmpty(teamAdminSet)) {
            allUsers.forEach(user -> {
                if (teamAdminSet.contains(user.getMailAddress())) {
                    user.setTeamAdmin(true);
                } else {
                    user.setTeamAdmin(false);
                }
            });
        }

        return allUsers;
    }

    public List<SysTeam> queryTeamsByUser(String mailAddress) {
        Set<SysTeam> teams = userTeamListMap.get(mailAddress);
        if (teams == null) {
            return null;
        }

        return new ArrayList<>(teams);
    }

    public boolean checkUserExistenceWithTeam(SysUser requestor, String teamId) {
        List<SysUser> users = queryUsersByTeam(teamId);
        if (CollectionUtils.isEmpty(users)) {
            return false;
        }
        if (users.size() == 1 && users.get(0).getMailAddress().equals(requestor.getMailAddress())) {
            return false;
        }

        return true;
    }

    public boolean checkTeamAdmin(String teamId, String mailAddress) {
        Set<String> teamAdmins = teamAdminListMap.get(teamId);
        return teamAdmins != null && teamAdmins.contains(mailAddress);
    }

    public boolean checkUserTeamRelation(String mailAddress, String teamId) {
        List<SysUser> users = queryUsersByTeam(teamId);
        for (SysUser user : users) {
            if (user.getMailAddress().equals(mailAddress)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkRequestorTeamRelation(SysUser requestor, String teamId) {
        return requestor.getTeamAdminMap().get(teamId) != null;
    }

    public boolean checkRequestorTeamAdmin(SysUser requestor, String teamId) {
        return requestor.getTeamAdminMap().get(teamId);
    }

    public List<CriteriaType> formTeamIdCriteria(Map<String, Boolean> teamAdminMap) {
        List<CriteriaType> criteriaTypes = new ArrayList<>();
        if (!CollectionUtils.isEmpty(teamAdminMap)) {
            CriteriaType teamIdCriteria = new CriteriaType();
            teamIdCriteria.setKey("teamId");
            teamIdCriteria.setOp(CriteriaType.OpType.In);
            JSONArray array = new JSONArray();
            array.addAll(teamAdminMap.keySet());
            teamIdCriteria.setValue(array.toJSONString());
            criteriaTypes.add(teamIdCriteria);
        }

        return criteriaTypes;
    }

    public void deleteTeam(SysTeam team) {
        List<UserTeamRelation> relations = userTeamRelationRepository.findAllByTeamId(team.getTeamId());
        relations.forEach(this::deleteUserTeamRelation);
        sysTeamService.deleteTeam(team);
    }
}
