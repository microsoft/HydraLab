// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import com.microsoft.hydralab.common.util.blob.DeviceNetworkBlobConstants;

public enum EntityType {
    SCREENSHOT("SCREENSHOT", DeviceNetworkBlobConstants.SCREENSHOT_CONTAINER_NAME),
    APP_FILE_SET("FILE_SET", DeviceNetworkBlobConstants.APP_FILE_CONTAINER_NAME),
    TEST_RESULT("TEST_RES", DeviceNetworkBlobConstants.TEST_RESULT_CONTAINER_NAME),
    AGENT_PACKAGE("AGENT_PKG", DeviceNetworkBlobConstants.AGENT_PACKAGE_CONTAINER_NAME),
    TEST_JSON("TEST_JSON", DeviceNetworkBlobConstants.TEST_JSON_CONTAINER_NAME);

    public String typeName;
    public String blobConstant;

    EntityType(String typeName, String blobConstant) {
        this.typeName = typeName;
        this.blobConstant = blobConstant;
    }
}