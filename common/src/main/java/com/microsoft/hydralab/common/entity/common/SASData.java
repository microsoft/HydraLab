// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Date;

/**
 * @author zhoule
 * @date 10/24/2022
 */

@Data
public class SASData {
    private String endpoint;
    private String signature;
    private OffsetDateTime expiredTime;
}
