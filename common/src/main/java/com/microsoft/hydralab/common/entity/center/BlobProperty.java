// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.center;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author zhoule
 * @date 11/15/2022
 */
@Data
@ConfigurationProperties(prefix = "app.blob")
@Component
public class BlobProperty {
    private String connection;
    private long SASExpiryTimeFont;
    private long SASExpiryTimeAgent;
    private long SASExpiryUpdate;
    private int fileLimitDay;
    private String CDNUrl;
}
