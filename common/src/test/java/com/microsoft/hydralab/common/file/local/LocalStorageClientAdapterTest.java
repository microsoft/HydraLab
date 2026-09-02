// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.file.local;

import com.microsoft.hydralab.common.file.AccessToken;
import com.microsoft.hydralab.common.file.impl.local.LocalStorageClientAdapter;
import com.microsoft.hydralab.common.file.impl.local.LocalStorageProperty;
import com.microsoft.hydralab.common.file.impl.local.LocalStorageToken;
import com.microsoft.hydralab.common.util.Const;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageClientAdapterTest {
    @Test
    void generatedTokensCarryPermissionScopeAndExpiry() {
        LocalStorageClientAdapter adapter = adapter();

        LocalStorageToken token = (LocalStorageToken) adapter.generateAccessTokenForFile(
                Const.FilePermission.READ, "packages/app.apk");

        assertEqualsPermissionAndScope(token);
        assertFalse(adapter.isAccessTokenExpired(token));
    }

    @Test
    void blankConfiguredTokenGetsUnpredictableDefault() {
        LocalStorageProperty first = new LocalStorageProperty();
        LocalStorageProperty second = new LocalStorageProperty();
        first.setToken("");
        second.setToken("");

        assertTrue(first.getToken().startsWith("token="));
        assertTrue(second.getToken().startsWith("token="));
        assertNotEquals(first.getToken(), second.getToken());
    }

    private LocalStorageClientAdapter adapter() {
        LocalStorageProperty property = new LocalStorageProperty();
        property.setEndpoint("http://localhost:9886/");
        property.setToken("token=center-secret");
        property.setFileExpiryDay(-1);
        return new LocalStorageClientAdapter(property);
    }

    private void assertEqualsPermissionAndScope(LocalStorageToken token) {
        assertTrue(token.getToken().startsWith("token="));
        assertTrue(token.getExpiresAtEpochSecond() > 0);
        assertEquals(Const.FilePermission.READ, token.getPermission());
        assertEquals("packages/app.apk", token.getFileUri());
    }
}
