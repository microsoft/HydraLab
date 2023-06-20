// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.controller;

import com.azure.core.annotation.QueryParam;
import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.center.service.DeviceGroupService;
import com.microsoft.hydralab.center.service.SysTeamService;
import com.microsoft.hydralab.center.service.SysUserService;
import com.microsoft.hydralab.center.service.UserTeamManagementService;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.DeviceGroup;
import com.microsoft.hydralab.common.entity.center.DeviceGroupRelation;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.AccessInfo;
import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.util.Const;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping
public class DeviceGroupController {
    @Resource
    DeviceGroupService deviceGroupService;
    @Resource
    DeviceAgentManagementService deviceAgentManagementService;
    @Resource
    private SysTeamService sysTeamService;
    @Resource
    private SysUserService sysUserService;
    @Resource
    private UserTeamManagementService userTeamManagementService;

    /**
     * Authenticated USER: all
     */
    @GetMapping(value = {"/api/deviceGroup/create"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<DeviceGroup> createGroup(@CurrentSecurityContext SysUser requestor,
                                           @QueryParam(value = "teamId") String teamId,
                                           @QueryParam(value = "groupName") String groupName) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
        }
        if (!userTeamManagementService.checkRequestorTeamAdmin(requestor, teamId)) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "User is not admin of this Team");
        }
        SysTeam team = sysTeamService.queryTeamById(teamId);
        if (team == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Team doesn't exist.");
        }
        if (groupName == null || "".equals(groupName)) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "groupName is required");
        }
        if (deviceGroupService.isGroupNameIllegal(groupName)) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "groupName should contain A-Z, a-z and 0-9 only");
        }
        String groupRealName = Const.DeviceGroup.GROUP_NAME_PREFIX + groupName;
        if (deviceGroupService.checkGroupName(groupRealName)) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "groupName already exist!");
        }

        DeviceGroup deviceGroup = deviceGroupService.createGroup(teamId, team.getTeamName(), groupName, requestor.getMailAddress());
        return Result.ok(deviceGroup);
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) group creator,
     * 3) admin of the TEAM that group is in
     */
    @GetMapping(value = {"/api/deviceGroup/delete"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("IllegalCatch")
    public Result deleteGroup(@CurrentSecurityContext SysUser requestor,
                              @QueryParam(value = "groupName") String groupName) {
        try {
            if (!deviceGroupService.checkGroupAuthorization(requestor, groupName, true)) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
            }

            deviceGroupService.deleteGroup(groupName);
            deviceAgentManagementService.removeGroup(groupName);
            return Result.ok("delete group success");
        } catch (IllegalArgumentException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) group creator,
     * 3) admin of the TEAM that group is in
     */
    @GetMapping(value = {"/api/deviceGroup/enableVerify"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("IllegalCatch")
    public Result<DeviceGroup> enableGroup(@CurrentSecurityContext SysUser requestor,
                                           @QueryParam(value = "groupName") String groupName) {
        try {
            if (!deviceGroupService.checkGroupAuthorization(requestor, groupName, true)) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
            }

            DeviceGroup deviceGroup = deviceGroupService.getGroupByName(groupName);
            deviceGroup.setIsPrivate(true);
            deviceGroupService.updateGroup(deviceGroup);
            return Result.ok(deviceGroup);
        } catch (IllegalArgumentException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) group creator,
     * 3) admin of the TEAM that group is in
     */
    @GetMapping(value = {"/api/deviceGroup/disableVerify"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("IllegalCatch")
    public Result<DeviceGroup> disableGroup(@CurrentSecurityContext SysUser requestor,
                                            @QueryParam(value = "groupName") String groupName) {
        try {
            if (!deviceGroupService.checkGroupAuthorization(requestor, groupName, true)) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
            }

            DeviceGroup deviceGroup = deviceGroupService.getGroupByName(groupName);
            deviceGroup.setIsPrivate(false);
            deviceGroupService.updateGroup(deviceGroup);
            return Result.ok(deviceGroup);
        } catch (IllegalArgumentException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    /**
     * Authenticated USER: all
     * Data access:
     * 1) For users with ROLE SUPER_ADMIN/ADMIN, return all data.
     * 2) For the rest users, return data that the group is in the user's TEAMs
     */
    @GetMapping(value = {"/api/deviceGroup/queryGroups"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<DeviceGroup>> queryGroups(@CurrentSecurityContext SysUser requestor) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
        }

        List<DeviceGroup> returnedGroups;
        if (sysUserService.checkUserAdmin(requestor)) {
            returnedGroups = deviceGroupService.queryAllGroups();
        } else {
            // return all DeviceGroups in TEAMs that user is in
            List<CriteriaType> criteriaTypes = userTeamManagementService.formTeamIdCriteria(requestor.getTeamAdminMap());
            if (criteriaTypes.size() == 0) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "User belongs to no TEAM, please contact administrator for binding TEAM");
            }

            returnedGroups = deviceGroupService.queryFilteredGroups(criteriaTypes);
        }

        return Result.ok(returnedGroups);
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) group creator,
     * 3) admin of the TEAM that group is in
     */
    @PostMapping(value = {"/api/deviceGroup/addRelation"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("IllegalCatch")
    public Result<DeviceGroupRelation> addRelation(@CurrentSecurityContext SysUser requestor,
                                                   @RequestParam(value = "groupName") String groupName,
                                                   @RequestParam(value = "deviceSerial") String deviceSerial,
                                                   @RequestParam(value = "accessKey", required = false) String accessKey) {
        try {
            if (!deviceGroupService.checkGroupAuthorization(requestor, groupName, true)) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
            }
            if (!deviceAgentManagementService.checkDeviceInfo(deviceSerial)) {
                return Result.error(HttpStatus.BAD_REQUEST.value(), "DeviceSerial is incorrect");
            }

            deviceAgentManagementService.addDeviceToGroup(groupName, deviceSerial, accessKey);
            DeviceGroupRelation deviceGroupRelation = deviceGroupService.saveRelation(groupName, deviceSerial);
            return Result.ok(deviceGroupRelation);
        } catch (IllegalArgumentException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) group creator,
     * 3) admin of the TEAM that group is in
     */
    @PostMapping(value = {"/api/deviceGroup/deleteRelation"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("IllegalCatch")
    public Result deleteRelation(@CurrentSecurityContext SysUser requestor,
                                 @RequestParam(value = "groupName", required = true) String groupName,
                                 @RequestParam(value = "deviceSerial", required = true) String deviceSerial) {
        try {
            if (!deviceGroupService.checkGroupAuthorization(requestor, groupName, true)) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
            }
            if (deviceGroupService.getRelation(groupName, deviceSerial) == null) {
                return Result.error(HttpStatus.BAD_REQUEST.value(), "DeviceSerial or groupName is incorrect");
            }

            deviceGroupService.deleteRelation(groupName, deviceSerial);
            deviceAgentManagementService.deleteDeviceFromGroup(groupName, deviceSerial);
            return Result.ok("delete Relation success!");
        } catch (IllegalArgumentException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    /**
     * Authenticated USER:
     * 1. If deviceIdentifier refers to a single device:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) device's agent creator,
     * 3) admin of the TEAM that device's agent is in
     * 2. If deviceIdentifier refers to a group:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) group creator,
     * 3) admin of the TEAM that group is in
     */
    @GetMapping(value = "/api/deviceGroup/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("IllegalCatch")
    public Result<AccessInfo> generateDeviceToken(@CurrentSecurityContext SysUser requestor,
                                                  @QueryParam(value = "deviceIdentifier") String deviceIdentifier) {
        try {
            if (deviceIdentifier == null || "".equals(deviceIdentifier)) {
                return Result.error(HttpStatus.BAD_REQUEST.value(), "deviceIdentifier is required");
            }
            if (deviceIdentifier.startsWith(Const.DeviceGroup.GROUP_NAME_PREFIX)) {
                if (!deviceGroupService.checkGroupAuthorization(requestor, deviceIdentifier, true)) {
                    return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
                }
            } else {
                if (!deviceAgentManagementService.checkDeviceAuthorization(requestor, deviceIdentifier)) {
                    return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
                }
            }

            AccessInfo accessInfo = new AccessInfo(deviceIdentifier);
            deviceAgentManagementService.updateAccessInfo(accessInfo);
            return Result.ok(accessInfo);
        } catch (IllegalArgumentException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) members of the TEAM that group is in
     */
    @GetMapping(value = "/api/deviceGroup/queryDeviceList", produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("IllegalCatch")
    public Result<List<DeviceInfo>> queryDevicesByGroup(@CurrentSecurityContext SysUser requestor,
                                                        @QueryParam(value = "groupName") String groupName) {
        try {
            if (!deviceGroupService.checkGroupAuthorization(requestor, groupName, false)) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
            }

            List<DeviceInfo> devices = deviceAgentManagementService.queryDevicesByGroup(groupName);
            return Result.ok(devices);
        } catch (IllegalArgumentException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }
}
