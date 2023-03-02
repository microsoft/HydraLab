// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.interceptor;

import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by bsp on 17/8/26.
 */
@Component
public class CorsInterceptor implements HandlerInterceptor {
    private final Logger logger = LoggerFactory.getLogger(CorsInterceptor.class);
    @Value("${ENV:dev}")
    private String ENV;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestURI = request.getRequestURI();
        String requestMethod = request.getMethod();

        if (LogUtils.isLegalStr(requestURI, Const.RegexString.URL, true) && LogUtils.isLegalStr(requestMethod, Const.RegexString.COMMON_STR, true)) {
            logger.info("Access added: " + requestURI + ", method " + requestMethod);// CodeQL [java/log-injection] False Positive: Has verified the string by regular expression
        } else {
            return false;
        }

        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "POST, GET");
        response.addHeader("Access-Control-Max-Age", "100");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type");
        response.addHeader("Access-Control-Allow-Credentials", "false");
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
