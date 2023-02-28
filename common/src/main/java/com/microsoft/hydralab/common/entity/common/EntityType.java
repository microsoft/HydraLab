// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import com.microsoft.hydralab.common.file.impl.azure.AzureBlobConstants;

public enum EntityType {
    APP_FILE_SET("FILE_SET", AzureBlobConstants.PKG_BLOB_NAME),
    TEST_RESULT("TEST_RES", AzureBlobConstants.TEST_RESULT_BLOB_NAME),
    AGENT_PACKAGE("AGENT_PKG", AzureBlobConstants.PKG_BLOB_NAME),
    TEST_JSON("TEST_JSON", AzureBlobConstants.TEST_JSON),
    SCREENSHOT("SCREENSHOT", AzureBlobConstants.IMAGES_BLOB_NAME);

    public String typeName;
    public String storageContainer;

    EntityType(String typeName, String storageContainer) {
        this.typeName = typeName;
        this.storageContainer = storageContainer;
    }
}
