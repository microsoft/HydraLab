// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.file.impl.blob;

import com.microsoft.hydralab.common.file.AccessToken;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * @author zhoule
 * @date 10/24/2022
 */

@Data
public class SASData implements AccessToken {
    private String endpoint;
    private String signature;
    private OffsetDateTime expiredTime;
    private int fileLimitDay;
    private String cdnUrl;
    private SASPermission sasPermission;
}
