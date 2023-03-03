// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import com.microsoft.hydralab.common.file.StorageProperties;

public enum EntityType {
    SCREENSHOT("SCREENSHOT"),
    APP_FILE_SET("FILE_SET"),
    TEST_RESULT("TEST_RES"),
    AGENT_PACKAGE("AGENT_PKG"),
    TEST_JSON("TEST_JSON");

    private String typeName;
    private String storageContainer;

    EntityType(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getStorageContainer() {
        return storageContainer;
    }

    public static void setInstanceContainer(StorageProperties storageProperties) {
        SCREENSHOT.storageContainer = storageProperties.getScreenshotContainerName();
        APP_FILE_SET.storageContainer = storageProperties.getAppFileContainerName();
        TEST_RESULT.storageContainer = storageProperties.getTestResultContainerName();
        AGENT_PACKAGE.storageContainer = storageProperties.getAgentPackageContainerName();
        TEST_JSON.storageContainer = storageProperties.getTestJsonContainerName();
    }
}
