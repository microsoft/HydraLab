// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableCaching
@EnableWebMvc
@EnableScheduling
@EnableJpaRepositories(basePackages = {"com.microsoft.hydralab.common.repository", "com.microsoft.hydralab.center.repository"})
@EntityScan(basePackages = {"com.microsoft.hydralab.common.entity.center", "com.microsoft.hydralab.common.entity.common"})
@PropertySource(value = {"classpath:version.properties"}, encoding = "utf-8")
@SuppressWarnings("HideUtilityClassConstructor")
public class RegisterCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegisterCenterApplication.class, args);
    }
}

