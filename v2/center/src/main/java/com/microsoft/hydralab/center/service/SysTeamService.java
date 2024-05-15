// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.repository.SysTeamRepository;
import com.microsoft.hydralab.center.repository.UserTeamRelationRepository;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import com.microsoft.hydralab.common.util.AttachmentService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SysTeamService {
    //save Team list <teamId, SysTeam>
    private final Map<String, SysTeam> teamListMap = new ConcurrentHashMap<>();
    @Resource
    SysTeamRepository sysTeamRepository;
    @Resource
    UserTeamRelationRepository userTeamRelationRepository;
    @Resource
    AgentManageService agentManageService;
    @Resource
    DeviceGroupService deviceGroupService;
    @Resource
    DeviceAgentManagementService deviceAgentManagementService;
    @Resource
    TestFileSetService testFileSetService;
    @Resource
    TestTaskService testTaskService;
    @Resource
    AttachmentService attachmentService;

    @PostConstruct
    public void initList() {
        List<SysTeam> teamList = sysTeamRepository.findAll();
        teamList.forEach(team -> teamListMap.put(team.getTeamId(), team));
    }

    public SysTeam createTeam(String teamName) {
        SysTeam sysTeam = new SysTeam();
        sysTeam.setTeamName(teamName);
        sysTeam.setCreateTime(new Date());
        sysTeam.setUpdateTime(new Date());

        teamListMap.put(sysTeam.getTeamId(), sysTeam);
        return sysTeamRepository.save(sysTeam);
    }

    @Transactional
    public SysTeam updateTeam(SysTeam sysTeam) {
        if (sysTeam == null) {
            return null;
        }

        sysTeam.setUpdateTime(new Date());
        updateTeamRelatedEntity(sysTeam.getTeamId(), sysTeam.getTeamName());
        SysTeam team = sysTeamRepository.save(sysTeam);

        teamListMap.put(sysTeam.getTeamId(), sysTeam);

        return team;
    }

    /**
     * Update TEAM for entities:
     * 1. AgentUser
     * 2. DeviceGroup
     * 3. AgentDeviceGroup
     * 4. TestFileSet
     * 5. TestTask
     * 6. TestJsonInfo
     */
    public void updateTeamRelatedEntity(String teamId, String teamName) {
        agentManageService.updateAgentTeam(teamId, teamName);
        deviceGroupService.updateGroupTeam(teamId, teamName);
        deviceAgentManagementService.updateAgentDeviceGroupTeam(teamId, teamName);
        testFileSetService.updateFileSetTeam(teamId, teamName);
        testTaskService.updateTaskTeam(teamId, teamName);
        attachmentService.updateTestJsonTeam(teamId, teamName);
    }

    public SysTeam queryTeamById(String teamId) {
        SysTeam team = teamListMap.get(teamId);
        if (team != null) {
            return team;
        }

        Optional<SysTeam> someTeam = sysTeamRepository.findById(teamId);
        if (someTeam.isPresent()) {
            team = someTeam.get();
            teamListMap.put(team.getTeamId(), team);
        }

        return team;
    }

    public SysTeam queryTeamByName(String teamName) {
        for (SysTeam team : new ArrayList<>(teamListMap.values())) {
            if (team.getTeamName().equals(teamName)) {
                return team;
            }
        }

        Optional<SysTeam> someTeam = sysTeamRepository.findByTeamName(teamName);
        SysTeam team = null;
        if (someTeam.isPresent()) {
            team = someTeam.get();
            teamListMap.put(team.getTeamId(), team);
        }

        return team;
    }

    public boolean checkTeamExistence(String teamId) {
        SysTeam team = queryTeamById(teamId);
        if (team == null) {
            return false;
        }

        return true;
    }

    public List<SysTeam> queryTeams() {
        return new ArrayList<>(teamListMap.values());
    }

    public void deleteTeam(SysTeam team) {
        sysTeamRepository.deleteById(team.getTeamId());
        teamListMap.remove(team.getTeamId());
    }

    public SysTeam getOrCreateDefaultTeam(String defaultTeamName) {
        SysTeam defaultTeam = queryTeamByName(defaultTeamName);
        if (defaultTeam == null) {
            defaultTeam = createTeam(defaultTeamName);
        }

        return defaultTeam;
    }
}
