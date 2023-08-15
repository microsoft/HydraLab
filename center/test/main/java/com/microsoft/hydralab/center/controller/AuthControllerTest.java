package com.microsoft.hydralab.center.controller;

import com.microsoft.hydralab.center.service.AuthTokenService;
import com.microsoft.hydralab.center.service.SecurityUserService;
import com.microsoft.hydralab.center.util.AuthUtil;
import com.microsoft.hydralab.center.util.SecretGenerator;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.AuthToken;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.util.Const;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthControllerTest {

    @Mock
    private AuthUtil authUtil;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @InjectMocks
    private AuthController authController;
    @Mock
    private SecretGenerator secretGenerator;
    @Mock
    private AuthTokenService authTokenService;
    @Mock
    private SecurityUserService securityUserService;
    @Mock
    private SysUser sysUser;
    @Mock
    private Result result;
    @Mock
    private AuthToken authToken;
    @Mock
    private List<AuthToken> authTokens;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAccessToken() {
        String code = "testCode";
        String redirectUrl = "testRedirectUrl";
        String accessToken = "testAccessToken";
        Mockito.when(authUtil.verifyCode(code)).thenReturn(accessToken);
        Mockito.when(request.getParameter("state")).thenReturn(redirectUrl);
        authController.getAccessToken(code, request, response);
        Mockito.verify(response).sendRedirect(redirectUrl);
    }

    @Test
    public void testDeleteToken() {
        when(authTokenService.deleteAuthToken(any(AuthToken.class))).thenReturn(result);
        Result actualResult = authController.deleteToken(sysUser, 1L);
        assertEquals(result, actualResult);
    }

    @Test
    public void testGetUserInfo() {
        SysUser requestor = new SysUser();
        requestor.setMailAddress("test@example.com");
        Result expectedResult = Result.ok(requestor);
        when(authController.getUserInfo(requestor)).thenReturn(expectedResult);
        Result actualResult = authController.getUserInfo(requestor);
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testGetUserPhoto() {
        SysUser sysUser = new SysUser();
        HttpServletResponse response = mock(HttpServletResponse.class);
        InputStream inputStream = mock(InputStream.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(authUtil.requestPhoto(anyString())).thenReturn(inputStream);
        when(response.getOutputStream()).thenReturn(outputStream);
        authController.getUserPhoto(sysUser, response);
        verify(authUtil, times(1)).requestPhoto(anyString());
        verify(response, times(1)).setContentType(MediaType.IMAGE_JPEG_VALUE);
        verify(outputStream, times(1)).write(any(byte[].class), anyInt(), anyInt());
        verify(outputStream, times(1)).flush();
        verify(outputStream, times(1)).close();
    }

}