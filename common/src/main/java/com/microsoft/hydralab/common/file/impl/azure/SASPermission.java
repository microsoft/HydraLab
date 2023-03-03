// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.file.impl.azure;

import java.time.temporal.ChronoUnit;

public enum SASPermission {
    /**
     * Define permission
     */
    WRITE("b", "co", "war"),
    READ("b", "o", "r");

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
                '}';
    }
}
