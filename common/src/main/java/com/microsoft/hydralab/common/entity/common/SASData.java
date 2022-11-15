// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * @author zhoule
 * @date 10/24/2022
 */

@Data
public class SASData {
    private String endpoint;
    private String signature;
    private OffsetDateTime expiredTime;
    private SASPermission sasPermission;

    public enum SASPermission {
        /**
         * Define permission
         */
        Write("b", "o", "war"),
        Read("b", "o", "r");

        public final String serviceStr, resourceStr, permissionStr;
        public long expiryTime;

        SASPermission(String serviceStr, String resourceStr, String permissionStr) {
            this.serviceStr = serviceStr;
            this.resourceStr = resourceStr;
            this.permissionStr = permissionStr;
        }

        public void setExpiryTime(long expiryTime) {
            this.expiryTime = expiryTime;
        }
    }
}
