// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.file.impl.local;

import com.microsoft.hydralab.common.file.StorageProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
    private String token;
    private int fileLimitDay;
}
