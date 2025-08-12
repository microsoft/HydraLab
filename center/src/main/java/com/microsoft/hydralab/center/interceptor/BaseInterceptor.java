// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.interceptor;

import com.microsoft.hydralab.center.service.AuthTokenService;
import com.microsoft.hydralab.center.util.AuthUtil;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.LogUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author shbu
 */
@Component
public class BaseInterceptor extends HandlerInterceptorAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseInterceptor.class);

    @Value("${spring.security.oauth2.enabled}")
    boolean enabledAuth;
    @Resource
    AuthUtil authUtil;
    @Resource
    AuthTokenService authTokenService;
    @Value("${app.storage.type}")
    private String storageType;
    @Value("${app.api-auth-mode}")
    private String apiAuthMode;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String remoteUser = request.getRemoteUser();
        String requestURI = request.getRequestURI();
        String sessionAuthToken = null;
        String authorizationToken;
        String aadIdToken;
        if (LogUtils.isLegalStr(requestURI, Const.RegexString.URL, true) && LogUtils.isLegalStr(remoteUser, Const.RegexString.MAIL_ADDRESS, true)) {
            LOGGER.info("New access from IP {}, host {}, user {}, for path {}", request.getRemoteAddr(), request.getRemoteHost(), remoteUser,
                    requestURI);// CodeQL [java/log-injection] False Positive: Has verified the string by regular expression
        } else {
            return false;
        }

        if (Const.LocalStorageConst.PATH_PREFIX_LIST.stream().anyMatch(requestURI::contains)) {
            return Const.StorageType.LOCAL.equals(storageType);
        }
        if (!enabledAuth) {
            authTokenService.loadDefaultUser(request.getSession());
            return true;
        }

        SecurityContext securityContext = (SecurityContext) request.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (securityContext != null) {
            SysUser userAuthentication = (SysUser) securityContext.getAuthentication();
            sessionAuthToken = userAuthentication.getAccessToken();
        }

        authorizationToken = request.getHeader("Authorization");
        if (authorizationToken != null) {
            authorizationToken = authorizationToken.replaceAll("Bearer ", "");
        }

        // For Azure AD authentication
        aadIdToken = request.getHeader("X-MS-TOKEN-AAD-ID-TOKEN");
        LOGGER.info("UserId: " + request.getHeader("X-MS-CLIENT-PRINCIPAL-ID"));
        LOGGER.info("UserName: " + request.getHeader("X-MS-CLIENT-PRINCIPAL-NAME"));

        // check is ignore
        if (!authUtil.isIgnore(requestURI)) {
            // invoked by API client
            if (!StringUtils.isEmpty(aadIdToken)) {
                if (authTokenService.checkAADToken(aadIdToken)) {
                    return true;
                } else {
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "unauthorized, error authorization code");
                }
            }

            // invoked by agent client
            if (!StringUtils.isEmpty(authorizationToken)) {
                if ("HYBRID".equals(apiAuthMode) && (authTokenService.checkAuthToken(authorizationToken) ||
                        authTokenService.setUserAuthByAppClientToken(authorizationToken))) {
                    return true;
                } else if ("SECRET".equals(apiAuthMode) && authTokenService.checkAuthToken(authorizationToken)) {
                    return true;
                } else if ("TOKEN".equals(apiAuthMode) && authTokenService.setUserAuthByAppClientToken(authorizationToken)) {
                    return true;
                } else {
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "unauthorized, error authorization code");
                }
            }

            // invoke by browser
            if (StringUtils.isEmpty(sessionAuthToken) || !authUtil.verifyToken(sessionAuthToken)) {
                if (requestURI.contains(Const.FrontEndPath.PREFIX_PATH)) {
                    String queryString = request.getQueryString();
                    if (StringUtils.isNotEmpty(queryString)
                            && queryString.startsWith(Const.FrontEndPath.REDIRECT_PARAM)
                            && LogUtils.isLegalStr(queryString.replace(Const.FrontEndPath.REDIRECT_PARAM + "=", ""), Const.RegexString.URL, false)
                            && LogUtils.isLegalStr(requestURI, Const.RegexString.URL, true)
                    ) {
                        response.sendRedirect(authUtil.getLoginUrl(requestURI,
                                queryString));// CodeQL [java/unvalidated-url-redirection] False Positive: Has verified the string by regular expression
                    } else {
                        response.sendRedirect(authUtil.getLoginUrl());
                    }
                } else {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setHeader("Location", authUtil.getLoginUrl());
                }
                return false;
            }
            //redirect
            String redirectUrl = request.getParameter(Const.FrontEndPath.REDIRECT_PARAM);
            if (StringUtils.isNotEmpty(redirectUrl) && LogUtils.isLegalStr(redirectUrl, Const.RegexString.URL, false)) {
                response.sendRedirect(Const.FrontEndPath.INDEX_PATH + Const.FrontEndPath.ANCHOR +
                        redirectUrl);// CodeQL [java/unvalidated-url-redirection] False Positive: Has verified the string by regular expression
                return false;
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        super.afterCompletion(request, response, handler, ex);
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        super.afterConcurrentHandlingStarted(request, response, handler);
    }
}
