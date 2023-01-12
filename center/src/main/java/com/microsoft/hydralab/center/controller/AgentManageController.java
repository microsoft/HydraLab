// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.center.service.*;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.AgentUser;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.AgentUpdateTask;
import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.util.AttachmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class AgentManageController {
    @Resource
    DeviceAgentManagementService deviceAgentManagementService;
    @Resource
    AttachmentService attachmentService;
    @Resource
    private AgentManageService agentManageService;
    @Resource
    private SysUserService sysUserService;
    @Resource
    private SysTeamService sysTeamService;
    @Resource
    private UserTeamManagementService userTeamManagementService;
    @Value("${center.version}")
    private String versionName;
    @Value("${center.versionCode}")
    private String versionCode;

    /**
     * Authenticated USER: all
     */
    @PostMapping("/api/agent/create")
    public Result<AgentUser> create(@CurrentSecurityContext SysUser requestor,
                                    @RequestParam(value = "teamId") String teamId,
                                    @RequestParam(value = "os", required = false) String os,
                                    @RequestParam(value = "name") String name) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        if (!userTeamManagementService.checkRequestorTeamRelation(requestor, teamId)) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "User doesn't belong to this Team");
        }
        SysTeam team = sysTeamService.queryTeamById(teamId);
        if (team == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Team doesn't exist.");
        }
        if (agentManageService.isAgentNameRegistered(name)) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "Agent name already registered");
        }

        AgentUser agentUserInfo = agentManageService.createAgent(teamId, team.getTeamName(), requestor.getMailAddress(), os, name);
        return Result.ok(agentUserInfo);
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) agent creator,
     * 3) admin of the TEAM that agent is in
     */
    @PostMapping("/api/agent/updateAgent")
    public Result updateAgentPackage(@CurrentSecurityContext SysUser requestor,
                                     @RequestParam(value = "agentId") String agentId,
                                     @RequestParam(value = "fileId") String fileId) {
        try {
            if (!agentManageService.checkAgentAuthorization(requestor, agentId)) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
            }

            deviceAgentManagementService.updateAgentPackage(agentId, fileId);
        } catch (Exception e) {
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }

        return Result.ok("Start Updating Success!");
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) agent creator,
     * 3) admin of the TEAM that agent is in
     */
    @PostMapping("/api/agent/restartAgent")
    public Result restartAgent(@CurrentSecurityContext SysUser requestor,
                               @RequestParam(value = "agentId") String agentId) {

        if (!agentManageService.checkAgentAuthorization(requestor, agentId)) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
        }

        deviceAgentManagementService.restartAgent(agentId);
        return Result.ok("Restart agent Success!");
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) agent creator,
     * 3) admin of the TEAM that agent is in
     */
    @GetMapping("/api/agent/getUpdateInfo/{agentId}")
    public Result getUpdateInfo(@CurrentSecurityContext SysUser requestor,
                                @PathVariable(value = "agentId") String agentId) {
        if (!agentManageService.checkAgentAuthorization(requestor, agentId)) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
        }

        AgentUpdateTask task = deviceAgentManagementService.getUpdateTask(agentId);
        if (task == null) {
            task = new AgentUpdateTask();
            task.setUpdateStatus(AgentUpdateTask.TaskConst.STATUS_NONE);
        }
        return Result.ok(task);
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @PostMapping("/api/agent/queryUpdateInfo")
    public Result queryUpdateInfo() {

        return Result.ok(deviceAgentManagementService.getUpdateTasks());
    }

    /**
     * Return AgentUser list with no secret
     * Authenticated USER: all
     * Data access:
     * 1) For users with ROLE SUPER_ADMIN/ADMIN, return all data.
     * 2) For the rest users, return data that the user is the agent's TEAM admin or creator
     */
    @GetMapping("/api/agent/list")
    public Result<List<AgentUser>> list(@CurrentSecurityContext SysUser requestor) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
        }

        List<AgentUser> returnedAgents;
        if (sysUserService.checkUserAdmin(requestor)) {
            returnedAgents = agentManageService.getAllAgentsWithoutCredentials();
        } else {
            // return all AgentUsers in TEAMs that user is team admin, and all AgentUser which mailAddress equals to user's
            List<CriteriaType> criteriaTypes = new ArrayList<>();
            Map<String, Boolean> teamAdminMap = requestor.getTeamAdminMap();

            if (!CollectionUtils.isEmpty(teamAdminMap)) {
                CriteriaType teamIdCriteria = new CriteriaType();
                teamIdCriteria.setKey("teamId");
                teamIdCriteria.setOp(CriteriaType.OpType.In);
                JSONArray array = new JSONArray();
                for (Map.Entry<String, Boolean> entry : teamAdminMap.entrySet()) {
                    // is team admin
                    if (entry.getValue()) {
                        array.add(entry.getKey());
                    }
                }
                if (array.size() > 0) {
                    teamIdCriteria.setValue(array.toJSONString());
                    criteriaTypes.add(teamIdCriteria);
                }
            }

            CriteriaType mailAddressCriteria = new CriteriaType();
            mailAddressCriteria.setKey("mailAddress");
            mailAddressCriteria.setOp(CriteriaType.OpType.Equal);
            mailAddressCriteria.setValue(requestor.getMailAddress());
            criteriaTypes.add(mailAddressCriteria);

            returnedAgents = agentManageService.getFilteredAgentsWithoutCredentials(criteriaTypes);
        }

        return Result.ok(returnedAgents);
    }

    /**
     * Authenticated USER: all
     * Data access: Return data for all requests, but with secret for only the user that is the agent's TEAM admin or creator
     */
    @GetMapping("/api/agent/{agentId}")
    public Result<AgentUser> getAgentInfo(@CurrentSecurityContext SysUser requestor,
                                          @PathVariable(value = "agentId") String agentId) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
        }
        AgentUser agentUser = agentManageService.getAgent(agentId);
        if (agentUser == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Agent user doesn't exist");
        }

        if (userTeamManagementService.checkRequestorTeamAdmin(requestor, agentUser.getTeamId()) || agentUser.getMailAddress().equals(requestor.getMailAddress())) {
            return Result.ok(agentUser);
        } else {
            agentUser.setSecret(null);
            return Result.ok(agentUser);
        }
    }

    /**
     * Fetch the agent application.yml config, so the user don't have to edit it by themselves.
     * Authenticated USER: all
     * Data access: For only the user that is the agent's TEAM admin or creator will be downloaded the agent config file with specific data
     */
    @GetMapping("/api/agent/downloadAgentConfigFile/{agentId}")
    public Result downloadAgentConfigFile(@CurrentSecurityContext SysUser requestor,
                                          @PathVariable(value = "agentId") String agentId,
                                          HttpServletResponse response) throws IOException {
        if (!agentManageService.checkAgentAuthorization(requestor, agentId)) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
        }
        File agentConfigFile = agentManageService.downloadAgentConfigFile(requestor, agentId);
        if (agentConfigFile == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "The file was not downloaded");
        }

        ServletOutputStream out = null;
        FileInputStream in = null;
        try {
            in = new FileInputStream(agentConfigFile);
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition","attachment;filename=" + agentConfigFile.getName());
            out = response.getOutputStream();
            int len = 0;
            byte[] buffer = new byte[1024 * 10];
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            response.flushBuffer();
            try {
                out.close();
                in.close();
                agentConfigFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return Result.ok();
    }

    /**
     * Authenticated USER: all
     */
    @GetMapping("/api/center/info")
    public Result getCenterInfo() {
        JSONObject data = new JSONObject();
        data.put("versionName", versionName);
        data.put("versionCode", versionCode);
        data.put("agentPkg", attachmentService.getLatestAgentPackage());
        return Result.ok(data);
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) agent creator,
     * 3) admin of the TEAM that agent is in
     */
    @GetMapping(value = {"/api/auth/deleteAgent/{agentId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result deleteAgent(@CurrentSecurityContext SysUser requestor,
                              @PathVariable(value = "agentId") String agentId) {
        if (!agentManageService.checkAgentAuthorization(requestor, agentId)) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
        }

        agentManageService.deleteAgent(agentManageService.getAgent(agentId));
        return Result.ok("Delete Success");
    }
}
