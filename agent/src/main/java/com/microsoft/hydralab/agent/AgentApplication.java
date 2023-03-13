// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = {"com.microsoft.hydralab"})
@EnableJpaRepositories(basePackages = {"com.microsoft.hydralab.common.repository",
        "com.microsoft.hydralab.agent.repository"})
@EntityScan(basePackages = {"com.microsoft.hydralab.common.entity.agent",
        "com.microsoft.hydralab.common.entity.common"})
@PropertySource(value = {"classpath:version.properties"}, encoding = "utf-8")
@Slf4j
@SuppressWarnings("hideutilityclassconstructor")
public class AgentApplication {

    public static void main(String[] args) {
        long time = System.currentTimeMillis();
        boolean headless = decideHeadlessFromArguments(args);
        ConfigurableApplicationContext context = new SpringApplicationBuilder(AgentApplication.class)
                .headless(headless)
                .run(args);
        log.info("\n*************************\nDevice Agent Startup success in {}\n*************************\n",
                System.currentTimeMillis() - time);
    }

    private static boolean decideHeadlessFromArguments(String[] args) {
        log.info("main function param: args value > %s \n {}", Arrays.asList(args).toString());
        boolean headless = false;
        for (String arg : args) {
            if (!arg.contains("spring.profiles.active=docker")) {
                continue;
            }
            System.setProperty("java.awt.headless", "true");
            headless = true;
            log.info(
                    "We are in the Docker environment, we will switch to headless mode, " +
                            "and the Windows App UI testing may have restricted support.");
            break;
        }
        return headless;
    }
}
