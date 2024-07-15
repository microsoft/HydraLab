// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.file.impl.azure;

import com.microsoft.hydralab.common.file.AccessToken;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * @author zhoule
 * @date 10/24/2022
 */

@Data
public class SASData implements AccessToken {
    @Deprecated
    private String signature;
    private String token;
    private String endpoint;
    private String container;
    private OffsetDateTime expiredTime;
    private int fileExpiryDay;
    private String cdnUrl;
    private SASPermission sasPermission;

    @Override
    public String getToken() {
        return token;
    }

    @Deprecated
    @Override
    public void copySignature() {
        signature = token;
    }
}
