// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SessionManageService {
    // user mailAddress -> http session list
    private final Map<String, List<HttpSession>> userHttpSession = new HashMap<>();
    @Resource
    SessionRegistry sessionRegistry;

    public List<HttpSession> getUserSessions(String mailAddress) {
        return userHttpSession.getOrDefault(mailAddress, new ArrayList<>());
    }

    public void putUserSession(String mailAddress, HttpSession httpSession) {
        List<HttpSession> userSessions = getUserSessions(mailAddress);
        Set<String> aliveSessionIds = sessionRegistry.getAllSessions(mailAddress, false).stream().map(SessionInformation::getSessionId).collect(Collectors.toSet());

        List<HttpSession> newSessionList = new ArrayList<>();
        userSessions.forEach(session -> {
            if (aliveSessionIds.contains(session.getId())) {
                newSessionList.add(session);
            }
        });

        if (!newSessionList.contains(httpSession)) {
            newSessionList.add(httpSession);
            userHttpSession.put(mailAddress, newSessionList);
        }
    }

}