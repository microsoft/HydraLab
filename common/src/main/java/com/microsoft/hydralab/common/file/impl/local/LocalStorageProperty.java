// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.file.impl.local;

import com.microsoft.hydralab.common.file.StorageProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author Li Shen
 * @date 3/6/2023
 */

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage.local")
@Component
public class LocalStorageProperty extends StorageProperties {
    private String endpoint;
    private String token = generateToken();
    private int fileExpiryDay;

    public void setToken(String token) {
        this.token = token == null || token.trim().isEmpty() ? generateToken() : token;
    }

    private static String generateToken() {
        return "token=" + UUID.randomUUID();
    }
}
