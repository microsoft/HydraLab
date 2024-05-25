// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@Profile({"auth"})
public class AADOauth2Configuration {


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(
                httpSecurity ->
                        httpSecurity.requestMatchers("/oauth2/**", "/login/**", "/agent/connect")
                                .permitAll()
                                .anyRequest().authenticated()
        ).oauth2Login(
                oauth2 ->
                        oauth2.defaultSuccessUrl("/portal/", true)
                                .failureUrl("/login?error")
                                .permitAll()
        );
        return http.build();
    }
}
