// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import com.microsoft.hydralab.common.file.ContainerConstants;

public enum EntityType {
    SCREENSHOT("SCREENSHOT"),
    APP_FILE_SET("FILE_SET"),
    TEST_RESULT("TEST_RES"),
    AGENT_PACKAGE("AGENT_PKG"),
    TEST_JSON("TEST_JSON");

    public String typeName;
    public String storageContainer;

    EntityType(String typeName) {
        this.typeName = typeName;
    }

    public static void setInstanceContainer(ContainerConstants constant) {
        SCREENSHOT.storageContainer = constant.getScreenshotContainerName();
        APP_FILE_SET.storageContainer = constant.getAppFileContainerName();
        TEST_RESULT.storageContainer = constant.getTestResultContainerName();
        AGENT_PACKAGE.storageContainer = constant.getAgentPackageContainerName();
        TEST_JSON.storageContainer = constant.getTestJsonContainerName();
    }
}
