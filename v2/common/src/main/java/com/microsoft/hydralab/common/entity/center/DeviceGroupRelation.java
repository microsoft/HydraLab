// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.center;

import lombok.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@Table(name = "device_group_relation")
@IdClass(DeviceGroupRelationId.class)
public class DeviceGroupRelation implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    private String groupName;
    @Id
    private String deviceSerial;

    public DeviceGroupRelation() {}
    public DeviceGroupRelation(String groupName, String deviceSerial) {
        this.deviceSerial = deviceSerial;
        this.groupName = groupName;
    }
}