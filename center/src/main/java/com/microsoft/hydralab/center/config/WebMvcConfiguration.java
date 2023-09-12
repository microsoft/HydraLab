// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.config;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.microsoft.hydralab.center.interceptor.BaseInterceptor;
import com.microsoft.hydralab.center.interceptor.CorsInterceptor;
import com.microsoft.hydralab.common.util.Const;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

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
        registry.addViewController("/portal/").setViewName("forward:" + Const.FrontEndPath.INDEX_PATH);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(baseInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/dist/**", "/store/**", "/static/**", Const.FrontEndPath.SWAGGER_DOC_PATH);
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
        // Sequence should be kept, currently this converter is only used for /v3/api-docs endpoint to avoid malformed content which should be in json format
        converters.add(new StringHttpMessageConverter());
        converters.add(fastJsonHttpMessageConverter);
    }

    @Bean
    public InternalResourceViewResolver defaultViewResolver() {
        // Add this to make sure redirect can be made when /swagger-ui.html would redirect to swagger-ui/index.html using the configUrl in /v3/api-docs
        return new InternalResourceViewResolver();
    }
}
