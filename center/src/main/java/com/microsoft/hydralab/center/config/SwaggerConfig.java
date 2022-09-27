// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by bsp on 17/12/16.
 */
@Configuration
@EnableSwagger2
//@Profile({"dev", "test"})
public class SwaggerConfig {
    @Value("${server.port}")
    private String port;

    @Bean
    public Docket docket() throws UnknownHostException {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("Hydra Lab API")
                .description("Hydra Lab")
                .termsOfServiceUrl("")
                .contact(new Contact("Microsoft Hydra Lab Support ", "", "hydra_lab_support@microsoft.com"))
                .version("1.0.0")
                .build();

        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .host(String.format("%s:%s", InetAddress.getLocalHost().getHostName(), port))
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.microsoft.devices.network.center.controller"))
                .paths(PathSelectors.any())
                .build();
    }
}
