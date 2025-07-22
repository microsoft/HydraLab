// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.repository.TeamAppRelationRepository;
import com.microsoft.hydralab.common.entity.center.TeamAppRelation;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TeamAppManagementService {
    // cache teamId -> App Client ID mapping <String, String>
    private final Map<String, Set<String>> teamAppListMap = new ConcurrentHashMap<>();
    // cache App Client ID -> teamId mapping <String, String>
    private final Map<String, String> appTeamListMap = new ConcurrentHashMap<>();
    @Resource
    private TeamAppRelationRepository teamAppRelationRepository;

    @PostConstruct
    public void initList() {
        List<TeamAppRelation> relationList = teamAppRelationRepository.findAll();
        relationList.forEach(relation -> {
            this.insertToCache(relation.getTeamId(), relation.getAppClientId());
        });
    }

    private void insertToCache(String teamId, String appClientId) {
        Set<String> clientIds = teamAppListMap.computeIfAbsent(teamId, k -> new HashSet<>());
        clientIds.add(appClientId);

        appTeamListMap.put(appClientId, teamId);
    }

    private void removeFromCache(TeamAppRelation relation) {
        Set<String> clientIdList = teamAppListMap.get(relation.getTeamId());
        if (clientIdList != null) {
            clientIdList.removeIf(clientId -> clientId.equals(relation.getAppClientId()));
        }
        appTeamListMap.remove(relation.getAppClientId());
    }

    public TeamAppRelation addTeamAppRelation(String teamId, String appClientId) {
        this.insertToCache(teamId, appClientId);

        TeamAppRelation teamAppRelation = new TeamAppRelation(teamId, appClientId);
        return teamAppRelationRepository.save(teamAppRelation);
    }

    public void deleteTeamAppRelation(TeamAppRelation relation) {
        removeFromCache(relation);
        teamAppRelationRepository.delete(relation);
    }

    public TeamAppRelation queryRelation(String appClientId, String teamId) {
        return teamAppRelationRepository.findByAppClientIdAndTeamId(appClientId, teamId).orElse(null);
    }

    public List<String> queryClientIdsByTeam(String teamId) {
        Set<String> clientIds = teamAppListMap.get(teamId);
        if (clientIds == null) {
            return null;
        }

        return new ArrayList<>(clientIds);
    }

    public String queryTeamIdByClientId(String appClientId) {
        return appTeamListMap.get(appClientId);
    }
}
