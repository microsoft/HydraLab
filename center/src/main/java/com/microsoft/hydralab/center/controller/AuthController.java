// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.controller;

import com.microsoft.hydralab.center.service.AuthTokenService;
import com.microsoft.hydralab.center.service.SecurityUserService;
import com.microsoft.hydralab.center.util.AuthUtil;
import com.microsoft.hydralab.center.util.SecretGenerator;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.AuthToken;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.LogUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@RestController
@RequestMapping
public class AuthController {

    @Resource
    AuthUtil authUtil;
    @Resource
    SecretGenerator secretGenerator;
    @Resource
    AuthTokenService authTokenService;
    @Resource
    SecurityUserService securityUserService;

    /**
     * Authenticated USER: all
     */
    @GetMapping(value = {"/api/auth"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public void getAccessToken(@RequestParam("code") String code, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String redirectUrl = Const.FrontEndPath.INDEX_PATH;
        String accessToken = authUtil.verifyCode(code);
        if (accessToken == null) {
            response.sendRedirect(authUtil.getLoginUrl());
            return;
        }

        securityUserService.addSessionAndUserAuth(authUtil.getLoginUserName(accessToken), accessToken, request.getSession());

        String state = request.getParameter("state");
        String prefix = Const.FrontEndPath.INDEX_PATH + "?" + Const.FrontEndPath.REDIRECT_PARAM + "=";

        if (StringUtils.isNotEmpty(state) && state.startsWith(prefix)) {
            String newUrl = state.replace(prefix, "");
            if (LogUtils.isLegalStr(newUrl, Const.RegexString.URL, false)) {
                redirectUrl = state;
            }
        }
        response.sendRedirect(redirectUrl);// CodeQL [java/unvalidated-url-redirection] False Positive: Has verified the string by regular expression
    }

    /**
     * Authenticated USER: all
     */
    @GetMapping(value = {"/api/auth/create"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<AuthToken> createAuthToken(@CurrentSecurityContext SysUser requestor) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
        }

        String token = secretGenerator.generateSecret();
        AuthToken authToken = new AuthToken();
        authToken.setToken(token);
        authToken.setCreator(requestor.getMailAddress());

        authToken = authTokenService.saveAuthToken(authToken);

        return Result.ok(authToken);
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @GetMapping(value = {"/api/auth/queryAll"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<AuthToken>> queryAuthToken() {

        List<AuthToken> authTokens = authTokenService.queryAuthToken();

        return Result.ok(authTokens);
    }

    /**
     * Authenticated USER: all
     */
    @GetMapping(value = {"/api/auth/querySelfToken"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<AuthToken>> queryAuthTokenByName(@CurrentSecurityContext SysUser requestor) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
        }

        List<AuthToken> authTokens = authTokenService.queryAuthTokenByName(requestor.getMailAddress());
        return Result.ok(authTokens);
    }

    /**
     * Authenticated USER: all
     * Data access: verify if requestor is the creator of the token
     */
    @GetMapping(value = {"/api/auth/deleteToken/{tokenId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result deleteToken(@CurrentSecurityContext SysUser requestor,
                              @PathVariable(value = "tokenId") Long tokenId) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
        }
        AuthToken authToken = authTokenService.getAuthToken(tokenId);
        if (authToken == null) {
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "tokenId error !");
        }
        if (!requestor.getMailAddress().equals(authToken.getCreator())) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        authTokenService.deleteAuthToken(authToken);
        return Result.ok("Delete Success");
    }

    /**
     * Authenticated USER: all
     */
    @GetMapping(value = {"/api/auth/getUser"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result getUserInfo(@CurrentSecurityContext SysUser requestor) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
        }

        return Result.ok(requestor);
    }

    /**
     * Authenticated USER: all
     */
    @GetMapping(value = {"/api/auth/getUserPhoto"}, produces = MediaType.IMAGE_JPEG_VALUE)
    @ResponseBody
    public void getUserPhoto(@CurrentSecurityContext SysUser requestor,
                             HttpServletResponse response) {
        try {
            InputStream inputStream = null;
            if (requestor == null || requestor.getAccessToken() == null) {
                inputStream = FileUtils.class.getClassLoader().getResourceAsStream(Const.Path.DEFAULT_PHOTO);
            } else {
                inputStream = authUtil.requestPhoto(requestor.getAccessToken());
            }
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes, 0, inputStream.available());
            response.setContentType(MediaType.IMAGE_JPEG_VALUE);
            OutputStream out = response.getOutputStream();
            out.write(bytes);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
