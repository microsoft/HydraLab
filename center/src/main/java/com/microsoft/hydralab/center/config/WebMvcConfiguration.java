// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.config;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.microsoft.hydralab.center.interceptor.BaseInterceptor;
import com.microsoft.hydralab.center.interceptor.CorsInterceptor;
import com.microsoft.hydralab.common.util.Const;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author shbu
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
    @Resource
    private BaseInterceptor baseInterceptor;
    @Resource
    private CorsInterceptor corsInterceptor;
    @Resource
    private FastJsonHttpMessageConverter fastJsonHttpMessageConverter;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/portal/").setViewName("forward:"+ Const.FrontPath.INDEX_PATH);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(baseInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/dist/**", "/store/**", "/static/**");
        registry.addInterceptor(corsInterceptor)
                .addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/portal/**")
                .addResourceLocations("classpath:/static/dist/");
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(fastJsonHttpMessageConverter);
    }
}
