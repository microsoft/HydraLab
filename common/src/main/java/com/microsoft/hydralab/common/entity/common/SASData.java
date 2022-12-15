// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * @author zhoule
 * @date 10/24/2022
 */

@Data
public class SASData {
    private String endpoint;
    private String signature;
    private OffsetDateTime expiredTime;
    private int fileLimitDay;
    private String cdnUrl;
    private SASPermission sasPermission;

    public enum SASPermission {
        /**
         * Define permission
         */
        Write("b", "o", "war"),
        Read("b", "o", "r");

        public final String serviceStr, resourceStr, permissionStr;
        public long expiryTime;
        public ChronoUnit timeUnit;

        SASPermission(String serviceStr, String resourceStr, String permissionStr) {
            this.serviceStr = serviceStr;
            this.resourceStr = resourceStr;
            this.permissionStr = permissionStr;
        }

        public void setExpiryTime(long expiryTime, String unit) {
            this.expiryTime = expiryTime;
            this.timeUnit = ChronoUnit.valueOf(unit);
        }

        @Override
        public String toString() {
            return "SASPermission{" +
                    "serviceStr='" + serviceStr + '\'' +
                    ", resourceStr='" + resourceStr + '\'' +
                    ", permissionStr='" + permissionStr + '\'' +
                    ", expiryTime=" + expiryTime +
                    ", timeUnit=" + timeUnit +
                    '}';
        }
    }
}
