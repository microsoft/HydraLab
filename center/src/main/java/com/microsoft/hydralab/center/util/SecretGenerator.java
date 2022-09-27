// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecretGenerator {
    BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    public String generateSecret() {
        return bCryptPasswordEncoder.encode(UUID.randomUUID().toString().replace("-", "")).replaceAll("\\W", "");
    }
}
