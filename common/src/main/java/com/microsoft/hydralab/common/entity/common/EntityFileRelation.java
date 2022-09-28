// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import com.microsoft.hydralab.common.util.blob.DeviceNetworkBlobConstants;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
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

    public EntityFileRelation() {
    }

    public EntityFileRelation(String entityId, String entityType, String fileId) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.fileId = fileId;
    }

    public enum EntityType {
        APP_FILE_SET("FILE_SET", DeviceNetworkBlobConstants.PKG_BLOB_NAME),
        TEST_RESULT("TEST_RES", DeviceNetworkBlobConstants.TEST_RESULT_BLOB_NAME),
        AGENT_PACKAGE("AGENT_PKG", DeviceNetworkBlobConstants.PKG_BLOB_NAME),
        TEST_JSON("TEST_JSON", DeviceNetworkBlobConstants.TEST_JSON);

        public String typeName;
        public String blobConstant;

        EntityType(String typeName, String blobConstant) {
            this.typeName = typeName;
            this.blobConstant = blobConstant;
        }
    }
}
