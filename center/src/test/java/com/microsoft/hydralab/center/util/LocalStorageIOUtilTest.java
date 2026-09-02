// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.util;

import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalStorageIOUtilTest {
    @Test
    void resolvesPathWithinLocalStorageRoot() {
        Path resolved = LocalStorageIOUtil.resolveFilePath("packages/app.apk");

        assertEquals("app.apk", resolved.getFileName().toString());
    }

    @Test
    void rejectsPathsOutsideLocalStorageRoot() {
        assertThrows(HydraLabRuntimeException.class,
                () -> LocalStorageIOUtil.resolveFilePath("../outside.txt"));
        assertThrows(HydraLabRuntimeException.class,
                () -> LocalStorageIOUtil.resolveFilePath("../local-backup/outside.txt"));
        assertThrows(HydraLabRuntimeException.class,
                () -> LocalStorageIOUtil.resolveFilePath("C:\\outside.txt"));
    }
}
