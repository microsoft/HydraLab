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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.microsoft.hydralab.center.interceptor.CorsInterceptor;

public class CorsInterceptorTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Object handler;
    private CorsInterceptor corsInterceptor;
    @Mock
    private ModelAndView modelAndView;
    @Mock
    private Exception ex;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        corsInterceptor = new CorsInterceptor();
    }

    @Test
    public void testPreHandle() {
        when(request.getRequestURI()).thenReturn("/test");
        when(request.getMethod()).thenReturn("GET");
        boolean result = corsInterceptor.preHandle(request, response, handler);
        assertTrue(result);
        verify(response).addHeader("Access-Control-Allow-Origin", "*");
        verify(response).addHeader("Access-Control-Allow-Methods", "POST, GET");
        verify(response).addHeader("Access-Control-Max-Age", "100");
        verify(response).addHeader("Access-Control-Allow-Headers", "Content-Type");
        verify(response).addHeader("Access-Control-Allow-Credentials", "false");
    }

    @Test
    public void testPostHandle() {
        corsInterceptor.postHandle(request, response, handler, modelAndView);
    }

    @Test
    public void testAfterCompletion() {
        corsInterceptor.afterCompletion(request, response, handler, ex);
    }
}