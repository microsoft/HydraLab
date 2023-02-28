// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.file.impl.azure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author zhoule
 * @date 11/15/2022
 */
@Data
@ConfigurationProperties(prefix = "app.storage.blob")
@Component
public class AzureBlobProperty {
    private String connection;
    private long SASExpiryTimeFront;
    private long SASExpiryTimeAgent;
    private long SASExpiryUpdate;
    private String timeUnit;
    private int fileLimitDay;
    private String CDNUrl;
}
