package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.repository.AuthTokenRepository;
import com.microsoft.hydralab.common.entity.center.AuthToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;
import java.util.ArrayList;

@Service
public class AuthTokenServiceTest {

    @Mock
    private AuthTokenRepository authTokenRepository;
    @Mock
    private SecurityUserService securityUserService;
    private AuthTokenService authTokenService;
    @Mock
    private HttpSession session;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        authTokenService = new AuthTokenService();
        authTokenService.authTokenRepository = authTokenRepository;
        authTokenService.securityUserService = securityUserService;
    }

    @Test
    public void testSaveAuthToken() {
        AuthToken authToken = new AuthToken();
        Mockito.when(authTokenRepository.save(authToken)).thenReturn(authToken);
        AuthToken result = authTokenService.saveAuthToken(authToken);
        Mockito.verify(authTokenRepository).save(authToken);
        assertEquals(authToken, result);
    }

    @Test
    public void testGetAuthToken() {
        Long tokenId = 1L;
        AuthToken authToken = new AuthToken();
        authToken.setId(tokenId);
        Optional<AuthToken> optionalAuthToken = Optional.of(authToken);
        when(authTokenRepository.findById(tokenId)).thenReturn(optionalAuthToken);
        AuthToken result = authTokenService.getAuthToken(tokenId);
        assertEquals(authToken, result);
    }

    @Test
    public void testDeleteAuthToken() {
        AuthToken authToken = new AuthToken();
        authTokenService.deleteAuthToken(authToken);
        verify(authTokenRepository, times(1)).delete(authToken);
    }

    @Test
    public void testCheckAuthToken() {
        String authToken = "token";
        List<AuthToken> authTokens = new ArrayList<>();
        AuthToken token = new AuthToken();
        token.setToken(authToken);
        authTokens.add(token);
        Mockito.when(authTokenRepository.queryByToken(authToken)).thenReturn(authTokens);
        Authentication authObj = Mockito.mock(Authentication.class);
        Mockito.when(securityUserService.loadUserAuthentication(Mockito.anyString(), Mockito.isNull())).thenReturn(authObj);
        boolean result = authTokenService.checkAuthToken(authToken);
        Mockito.verify(authTokenRepository).queryByToken(authToken);
        Mockito.verify(securityUserService).loadUserAuthentication(Mockito.anyString(), Mockito.isNull());
        Mockito.verify(SecurityContextHolder.getContext()).setAuthentication(authObj);
        Assert.assertTrue(result);
    }

    @Test
    public void testLoadDefaultUser() {
        AuthToken authToken = new AuthToken();
        Mockito.when(authTokenRepository.save(Mockito.any(AuthToken.class))).thenReturn(authToken);
        authTokenService.loadDefaultUser(session);
        Mockito.verify(securityUserService).addDefaultUserSession(session);
    }
}