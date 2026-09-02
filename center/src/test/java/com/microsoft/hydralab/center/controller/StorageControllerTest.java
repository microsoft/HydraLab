// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.controller;

import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageControllerTest {
    @Test
    void acceptsOnlyStrictBearerAuthorizationHeader() {
        assertEquals("token=issued", StorageController.extractBearerToken("Bearer token=issued"));
        assertNull(StorageController.extractBearerToken("token=issued"));
        assertNull(StorageController.extractBearerToken("BearerBearer token=issued"));
        assertNull(StorageController.extractBearerToken("Bearer "));
        assertNull(StorageController.extractBearerToken("Bearer  "));
    }

    @Test
    void screenshotTokenPathCannotEscapeScreenshotStorage() {
        assertEquals("images/devices/screenshots/device/screen.png",
                StorageController.getScreenshotStoragePath("/devices/screenshots/device/screen.png"));
        assertThrows(HydraLabRuntimeException.class,
                () -> StorageController.getScreenshotStoragePath(
                        "/devices/screenshots/../../../pkgstore/private.apk"));
    }
}
