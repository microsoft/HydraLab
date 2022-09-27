// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.util.AuthUtil;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.entity.center.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SecurityUserService {
    // no need to use
    @Resource
    SysUserService sysUserService;
    @Resource
    SysRoleService sysRoleService;
    @Resource
    SysTeamService sysTeamService;
    @Resource
    AuthUtil authUtil;
    @Resource
    RolePermissionManagementService rolePermissionManagementService;
    @Resource
    UserTeamManagementService userTeamManagementService;
    @Resource
    SessionManageService sessionManageService;
    @Resource
    SessionRegistry sessionRegistry;

    public Authentication loadUserAuthentication(String mailAddress, String accessToken) {
        SysUser user = sysUserService.queryUserByMailAddress(mailAddress);
        if (user == null) {
            if (StringUtils.isEmpty(accessToken)) {
                return null;
            }

            // load user entity for first-time login USER with Default TEAM and ROLE
            SysRole defaultRole = sysRoleService.getOrCreateDefaultRole(Const.DefaultRole.USER, 100);
            SysTeam defaultTeam = sysTeamService.getOrCreateDefaultTeam(Const.DefaultTeam.DEFAULT_TEAM_NAME);
            String displayName = authUtil.getLoginUserDisplayName(accessToken);

            user = sysUserService.createUserWithDefaultRole(displayName, mailAddress, defaultRole.getRoleId());
            userTeamManagementService.addUserTeamRelation(defaultTeam.getTeamId(), user, false);
        }

        // load accessToken when request comes from portal
        if (StringUtils.isEmpty(user.getAccessToken())) {
            user.setAccessToken(accessToken);
        }

        // load bound relation and teamAdmin identity
        loadTeamAndAdmin(user);
        // load ROLE/PERMISSION as authorities of a user, used for different scopes of permission checking.
        loadGrantedAuthority(user);

        return user;
    }

    private void loadTeamAndAdmin(SysUser user) {
        List<UserTeamRelation> relations = userTeamManagementService.queryTeamRelationsByMailAddress(user.getMailAddress());
        Map<String, Boolean> userTeamAdminMap = new HashMap<>();
        relations.forEach(relation -> userTeamAdminMap.put(relation.getTeamId(), relation.isTeamAdmin()));
        user.setTeamAdminMap(userTeamAdminMap);
    }

    private void loadGrantedAuthority(SysUser user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        user.setAuthorities(authorities);

        SysRole userRole = sysRoleService.queryRoleById(user.getRoleId());
        authorities.add(new SimpleGrantedAuthority(userRole.getAuthority()));
        List<SysPermission> permissions = rolePermissionManagementService.queryPermissionsByRole(user.getRoleId());
        if (!CollectionUtils.isEmpty(permissions)) {
            permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.getAuthority())));
        }
    }

    public void addSessionAndUserAuth(String mailAddress, String accessToken, HttpSession session) {
        sessionManageService.putUserSession(mailAddress, session);
        Authentication authObj = loadUserAuthentication(mailAddress, accessToken);
        SecurityContextHolder.getContext().setAuthentication(authObj);
        // only register session in front-end requests to avoid repeatable add-delete action of session storage for API requests
        sessionRegistry.registerNewSession(session.getId(), mailAddress);
    }

    public void reloadUserAuthentication(String mailAddress, String updateContent) {
        List<HttpSession> sessions = sessionManageService.getUserSessions(mailAddress);
        sessions.forEach(session -> reloadUserAuthenticationToSession(session, updateContent));
    }

    public void reloadUserAuthenticationToSession(HttpSession session, String updateContent) {
        try {
            SecurityContext securityContext = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            Authentication authentication = securityContext.getAuthentication();
            if (authentication instanceof SysUser) {
                SysUser authUser = (SysUser) authentication;

                switch (updateContent) {
                    case Const.AUTH_COMPONENT.TEAM:
                        loadTeamAndAdmin(authUser);
                        break;
                    case Const.AUTH_COMPONENT.ROLE:
                        loadGrantedAuthority(authUser);
                        break;
                    default:
                        authentication = this.loadUserAuthentication(authUser.getMailAddress(), authUser.getAccessToken());
                        securityContext.setAuthentication(authentication);
                        break;
                }
            }
        } catch (IllegalStateException exception) {
            log.warn("Session already invalidated, no need to be reloaded");
        }
    }
}
