// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import com.microsoft.hydralab.common.file.impl.azure.BlobConstants;

public enum EntityType {
    APP_FILE_SET("FILE_SET", BlobConstants.PKG_BLOB_NAME),
    TEST_RESULT("TEST_RES", BlobConstants.TEST_RESULT_BLOB_NAME),
    AGENT_PACKAGE("AGENT_PKG", BlobConstants.PKG_BLOB_NAME),
    TEST_JSON("TEST_JSON", BlobConstants.TEST_JSON),
    SCREENSHOT("SCREENSHOT", BlobConstants.IMAGES_BLOB_NAME);

    public String typeName;
    public String storageContainer;

    EntityType(String typeName, String storageContainer) {
        this.typeName = typeName;
        this.storageContainer = storageContainer;
    }
}
