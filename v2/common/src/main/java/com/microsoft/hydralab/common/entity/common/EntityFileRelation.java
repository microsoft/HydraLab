// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.io.Serializable;

@Data
@Entity
@IdClass(FileRelationId.class)
public class EntityFileRelation implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    private String entityId;
    @Id
    private String entityType;
    @Id
    private String fileId;

    @Column(columnDefinition = "int default 0")
    private int fileOrder;

    public EntityFileRelation() {
    }

    public EntityFileRelation(String entityId, String entityType, String fileId) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.fileId = fileId;
    }
}
