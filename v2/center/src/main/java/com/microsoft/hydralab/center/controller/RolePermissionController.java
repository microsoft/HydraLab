// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.controller;

import com.microsoft.hydralab.center.service.RolePermissionManagementService;
import com.microsoft.hydralab.center.service.SecurityUserService;
import com.microsoft.hydralab.center.service.SysPermissionService;
import com.microsoft.hydralab.center.service.SysRoleService;
import com.microsoft.hydralab.center.service.SysUserService;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.RolePermissionRelation;
import com.microsoft.hydralab.common.entity.center.SysPermission;
import com.microsoft.hydralab.common.entity.center.SysRole;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.util.Const;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping
public class RolePermissionController {
    @Resource
    SysRoleService sysRoleService;
    @Resource
    SysUserService sysUserService;
    @Resource
    SysPermissionService sysPermissionService;
    @Resource
    RolePermissionManagementService rolePermissionManagementService;
    @Resource
    SecurityUserService securityUserService;

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @PostMapping(value = {"/api/role/create"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<SysRole> createRole(@CurrentSecurityContext SysUser requestor,
                                      @RequestParam("roleName") String roleName,
                                      @RequestParam("authLevel") int authLevel) {
        SysRole requestorRole = sysRoleService.getRequestorRole(requestor);
        if (requestorRole == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        if (!sysRoleService.isAuthLevelValid(authLevel)) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Auth level not valid, input value larger than 0.");
        }
        if (requestorRole.getAuthLevel() >= authLevel) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Current user must has a role with higher permission to create a role with the target auth level.");
        }
        SysRole sysRole = sysRoleService.queryRoleByName(roleName);
        if (sysRole != null) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "Role name exists.");
        }

        sysRole = sysRoleService.createRole(roleName, authLevel);
        return Result.ok(sysRole);
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @PostMapping(value = {"/api/role/delete"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result deleteRole(@CurrentSecurityContext SysUser requestor,
                             @RequestParam("roleId") String roleId) {
        SysRole requestorRole = sysRoleService.getRequestorRole(requestor);
        if (requestorRole == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        SysRole sysRole = sysRoleService.queryRoleById(roleId);
        if (sysRole == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Role doesn't exist.");
        }
        if (requestorRole.getAuthLevel() >= sysRole.getAuthLevel()) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Current user must has a role with higher permission to delete a role with the target auth level.");
        }
        if (sysUserService.checkUserExistenceWithRole(sysRole.getRoleId())) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "There are still users of this ROLE, operation is forbidden.");
        }

        sysRoleService.deleteRole(sysRole);
        return Result.ok("Delete Role success!");
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @PostMapping(value = {"/api/role/update"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<SysRole> updateRole(@CurrentSecurityContext SysUser requestor,
                                      @RequestParam("roleId") String roleId,
                                      @RequestParam(value = "roleName", required = false) String roleName,
                                      @RequestParam(value = "authLevel", required = false) Integer authLevel) {
        SysRole requestorRole = sysRoleService.getRequestorRole(requestor);
        if (requestorRole == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        SysRole sysRole = sysRoleService.queryRoleById(roleId);
        if (sysRole == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Role doesn't exist.");
        }
        if (requestorRole.getAuthLevel() >= sysRole.getAuthLevel()
                || (authLevel != null && requestorRole.getAuthLevel() >= authLevel)) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Current user must has a role with higher permission to update a role with the original/target auth level.");
        }

        if (authLevel != null) {
            if (!sysRoleService.isAuthLevelValid(authLevel)) {
                return Result.error(HttpStatus.BAD_REQUEST.value(), "Auth level not valid, input value should be larger than 0.");
            }
            sysRole.setAuthLevel(authLevel);
        }
        if (roleName != null) {
            SysRole someRole = sysRoleService.queryRoleByName(roleName);
            if (someRole != null && !someRole.getRoleId().equals(roleId)) {
                return Result.error(HttpStatus.FORBIDDEN.value(), "Role name exists.");
            }
            sysRole.setRoleName(roleName);
        }

        sysRoleService.updateRole(sysRole);
        return Result.ok(sysRole);
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @GetMapping(value = {"/api/role/list"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<SysRole>> getRoleList() {
        return Result.ok(sysRoleService.queryRoles());
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @PostMapping(value = {"/api/role/switch"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<SysUser> switchUserRole(@RequestParam("roleId") String roleId, @RequestParam("userId") String userId) {
        SysUser sysUser = sysUserService.queryUserById(userId);
        if (sysUser == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "User id is wrong.");
        }
        SysRole sysRole = sysRoleService.queryRoleById(roleId);
        if (sysRole == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Role id is wrong.");
        }

        sysUserService.switchUserRole(sysUser, sysRole.getRoleId(), sysRole.getRoleName());
        securityUserService.reloadUserAuthentication(sysUser.getMailAddress(), Const.AuthComponent.ROLE);
        return Result.ok(sysUser);
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @PostMapping(value = {"/api/permission/create"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<SysPermission> createPermission(@RequestParam("permissionType") String permissionType,
                                                  @RequestParam("permissionContent") String permissionContent) {
        if (!(permissionType.equals(Const.PermissionType.API) || permissionType.equals(Const.PermissionType.METHOD))) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Permission type is wrong.");
        }
        SysPermission permission = sysPermissionService.queryPermissionByContent(permissionContent);
        if (permission != null) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "Permission exists.");
        }

        permission = sysPermissionService.createPermission(permissionType, permissionContent);
        return Result.ok(permission);
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @PostMapping(value = {"/api/permission/delete"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result deletePermission(@RequestParam("permissionId") String permissionId) {
        SysPermission permission = sysPermissionService.queryPermissionById(permissionId);
        if (permission == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Permission doesn't exist.");
        }

        sysPermissionService.deletePermission(permission);
        return Result.ok("Delete Permission success!");
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @PostMapping(value = {"/api/permission/update"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<SysPermission> updatePermission(@RequestParam("permissionId") String permissionId,
                                                  @RequestParam(value = "permissionType", required = false) String permissionType,
                                                  @RequestParam(value = "permissionContent", required = false) String permissionContent) {
        SysPermission permission = sysPermissionService.queryPermissionById(permissionId);
        if (permission == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Permission doesn't exist.");
        }
        if (permissionContent != null) {
            SysPermission somePermission = sysPermissionService.queryPermissionByContent(permissionContent);
            if (somePermission != null && !somePermission.getPermissionId().equals(permissionId)) {
                return Result.error(HttpStatus.FORBIDDEN.value(), "Permission content exists.");
            }
            permission.setPermissionContent(permissionContent);
        }
        if (permissionType != null) {
            permission.setPermissionType(permissionType);
        }

        sysPermissionService.updatePermission(permission);
        return Result.ok(permission);
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @GetMapping(value = {"/api/permission/list"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<SysPermission>> getPermissionList() {
        return Result.ok(sysPermissionService.queryPermissions());
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @PostMapping(value = {"/api/rolePermission/addRelation"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<RolePermissionRelation> addRolePermissionRelation(@RequestParam("roleId") String roleId, @RequestParam("permissionId") String permissionId) {
        SysRole role = sysRoleService.queryRoleById(roleId);
        if (role == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Role id is wrong.");
        }
        SysPermission permission = sysPermissionService.queryPermissionById(permissionId);
        if (permission == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Permission id is wrong.");
        }
        RolePermissionRelation relation = rolePermissionManagementService.queryRelation(roleId, permissionId);
        if (relation != null) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "Relation exists.");
        }

        return Result.ok(rolePermissionManagementService.addRolePermissionRelation(roleId, permissionId));
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @PostMapping(value = {"/api/rolePermission/deleteRelation"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result deleteRolePermissionRelation(@RequestParam("roleId") String roleId, @RequestParam("permissionId") String permissionId) {
        SysRole role = sysRoleService.queryRoleById(roleId);
        if (role == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Role id is wrong.");
        }
        SysPermission permission = sysPermissionService.queryPermissionById(permissionId);
        if (permission == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Permission id is wrong.");
        }
        RolePermissionRelation relation = rolePermissionManagementService.queryRelation(roleId, permissionId);
        if (relation == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Relation doesn't exist.");
        }

        rolePermissionManagementService.deleteRolePermissionRelation(relation);
        return Result.ok("delete role-permission relation success!");
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @PostMapping(value = {"/api/rolePermission/queryPermissions"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<SysPermission>> queryPermissionsByRole(@RequestParam("roleId") String roleId) {
        List<SysPermission> permissionList = rolePermissionManagementService.queryPermissionsByRole(roleId);
        return Result.ok(permissionList);
    }
}
