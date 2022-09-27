// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.common.entity.center.AuthToken;
import com.microsoft.hydralab.center.repository.AuthTokenRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

@Service
public class AuthTokenService {

    @Resource
    AuthTokenRepository authTokenRepository;
    @Resource
    SecurityUserService securityUserService;

    public AuthToken saveAuthToken(AuthToken authToken) {
        authToken = authTokenRepository.save(authToken);
        return authToken;
    }

    public AuthToken getAuthToken(Long tokenId) {
        Optional<AuthToken> authToken = authTokenRepository.findById(tokenId);
        if (!authToken.isPresent()) {
            return null;
        }
        return authToken.get();
    }

    public void deleteAuthToken(AuthToken authToken) {
        authTokenRepository.delete(authToken);
    }

    public List<AuthToken> queryAuthTokenByName(String name) {
        List<AuthToken> authTokens = authTokenRepository.queryByCreator(name);

        return authTokens;
    }

    public List<AuthToken> queryAuthToken() {
        List<AuthToken> authTokens = authTokenRepository.findAll();

        return authTokens;
    }

    public boolean checkAuthToken(String authToken) {
        List<AuthToken> authTokens = authTokenRepository.queryByToken(authToken);
        if (authTokens.size() > 0) {
            Authentication authObj = securityUserService.loadUserAuthentication(authTokens.get(0).getCreator(), null);
            if (authObj == null) {
                return false;
            }
            SecurityContextHolder.getContext().setAuthentication(authObj);
            return true;
        } else return false;
    }
}
