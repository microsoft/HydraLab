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

    private final String serviceStr;
    private final String resourceStr;
    private final String permissionStr;
    private long expiryTime;
    private ChronoUnit timeUnit;

    SASPermission(String serviceStr, String resourceStr, String permissionStr) {
        this.serviceStr = serviceStr;
        this.resourceStr = resourceStr;
        this.permissionStr = permissionStr;
    }

    public String getServiceStr() {
        return serviceStr;
    }

    public String getResourceStr() {
        return resourceStr;
    }

    public String getPermissionStr() {
        return permissionStr;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public ChronoUnit getTimeUnit() {
        return timeUnit;
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
