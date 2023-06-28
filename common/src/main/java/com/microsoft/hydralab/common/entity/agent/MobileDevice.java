// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.agent;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Data
@Entity
public class MobileDevice {
    private String serialNum;
    private String name;
    private String manufacturer;
    private String model;
    private String osVersion;
    private String buildNumber;
    private String screenSize;
    private int screenDensity;
    private String osSDKInt;
    private Boolean isPrivate = false;
    @Id
    private String id;
    private final Date ingestTime;

    public MobileDevice() {
        id = UUID.randomUUID().toString();
        ingestTime = new Date();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MobileDevice that = (MobileDevice) o;
        return Objects.equals(serialNum, that.serialNum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serialNum);
    }
}

