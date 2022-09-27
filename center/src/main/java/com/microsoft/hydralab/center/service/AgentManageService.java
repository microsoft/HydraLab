// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.repository.AgentUserRepository;
import com.microsoft.hydralab.center.util.SecretGenerator;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.entity.center.AgentUser;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.util.CriteriaTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class AgentManageService {
    @Resource
    AgentUserRepository agentUserRepository;
    @Resource
    private UserTeamManagementService userTeamManagementService;
    @Resource
    private SysUserService sysUserService;

    public AgentUser createAgent(String teamId, String teamName, String mailAddress, String os, String name) {
        AgentUser agentUserInfo = new AgentUser();
        agentUserInfo.setMailAddress(mailAddress);
        agentUserInfo.setOs(os);
        agentUserInfo.setName(name);
        agentUserInfo.setDeviceType(AgentUser.DeviceType.ANDROID);
        agentUserInfo.setTeamId(teamId);
        agentUserInfo.setTeamName(teamName);

        SecretGenerator secretGenerator = new SecretGenerator();
        String agentSecret = secretGenerator.generateSecret();
        agentUserInfo.setSecret(agentSecret);

        // add new validated agent info to AgentUserRepository
        agentUserRepository.saveAndFlush(agentUserInfo);

        return agentUserInfo;
    }

    public AgentUser getAgent(String agentId) {
        Optional<AgentUser> agent = agentUserRepository.findById(agentId);
        return agent.orElse(null);
    }

    public void deleteAgent(AgentUser agentUser) {
        agentUserRepository.delete(agentUser);
    }

    public boolean isAgentNameRegistered(String agentName) {
        Optional<AgentUser> agentUsers = agentUserRepository.findByName(agentName);
        if (agentUsers.isPresent()) {
            return true;
        }
        return false;
    }

    public List<AgentUser> getAllAgentsWithoutCredentials() {
        List<AgentUser> all = agentUserRepository.findAll();
        for (AgentUser agentUser : all) {
            agentUser.setSecret(null);
        }
        return all;
    }

    public List<AgentUser> getFilteredAgentsWithoutCredentials(List<CriteriaType> queryParams) {
        List<AgentUser> agents = getFilteredAgents(queryParams);
        for (AgentUser agentUser : agents) {
            agentUser.setSecret(null);
        }
        return agents;
    }

    public List<AgentUser> getFilteredAgents(List<CriteriaType> queryParams) {
        Specification<AgentUser> spec = null;
        if (queryParams != null && queryParams.size() > 0) {
            spec = new CriteriaTypeUtil<AgentUser>().transferToSpecification(queryParams, true);
        }

        List<AgentUser> agentUsers = agentUserRepository.findAll(spec);
        return agentUsers;
    }

    public List<AgentUser> getAgentsByTeamId(String teamId) {
        return agentUserRepository.findAllByTeamId(teamId);
    }

    public List<AgentUser> getAgentsByUserMail(String mailAddress) {
        List<AgentUser> agents = agentUserRepository.findAllByMailAddress(mailAddress);
        return agents;
    }

    public boolean checkAgentAuthorization(SysUser requestor, String agentId) {
        if (requestor == null) {
            return false;
        }

        AgentUser agentUser = getAgent(agentId);
        if (agentUser == null) {
            return false;
        }

        // agent owner
        if (agentUser.getMailAddress().equals(requestor.getMailAddress())) {
            return true;
        }

        // ROLE = SUPER_ADMIN / ADMIN
        if (sysUserService.checkUserAdmin(requestor)) {
            return true;
        }

        // TEAM_ADMIN of current TEAM
        return userTeamManagementService.checkRequestorTeamAdmin(requestor, agentUser.getTeamId());
    }

    public void updateAgentTeam(String teamId, String teamName){
         List<AgentUser> agents = getAgentsByTeamId(teamId);
         agents.forEach(agent -> agent.setTeamName(teamName));

         agentUserRepository.saveAll(agents);
    }
}
