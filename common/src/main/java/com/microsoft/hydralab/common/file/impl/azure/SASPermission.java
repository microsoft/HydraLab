// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.file.impl.azure;

import com.azure.storage.blob.sas.BlobContainerSasPermission;

import java.time.temporal.ChronoUnit;

public enum SASPermission {
    /**
     * Define permission
     */
    WRITE(true, true),
    READ(true, false);

    public long expiryTime;
    public ChronoUnit timeUnit;

    public final BlobContainerSasPermission permission = new BlobContainerSasPermission();

    SASPermission(boolean read, boolean write) {
        if (read) {
            this.permission.setReadPermission(true);
        }
        if (write) {
            this.permission.setWritePermission(true);
        }
    }

    public void setExpiryTime(long expiryTime, String unit) {
        this.expiryTime = expiryTime;
        this.timeUnit = ChronoUnit.valueOf(unit);
    }

    @Override
    public String toString() {
        return "SASPermission{" +
                "expiryTime=" + expiryTime +
                ", timeUnit=" + timeUnit +
                ", permission=" + permission +
                '}';
    }
}
