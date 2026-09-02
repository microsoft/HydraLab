// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.file.impl.local.LocalStorageProperty;
import com.microsoft.hydralab.common.file.impl.local.LocalStorageToken;
import com.microsoft.hydralab.common.util.Const;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageTokenManageServiceTest {
    private StorageTokenManageService tokenService;
    private StorageServiceClientProxy storageClient;

    @BeforeEach
    void setUp() {
        tokenService = new StorageTokenManageService();
        storageClient = mock(StorageServiceClientProxy.class);
        tokenService.storageServiceClientProxy = storageClient;

        LocalStorageProperty localStorageProperty = new LocalStorageProperty();
        localStorageProperty.setToken("token=center-secret");
        tokenService.localStorageProperty = localStorageProperty;
    }

    @Test
    void rejectsTokensThatWereNotIssued() {
        assertFalse(tokenService.validateAccessToken("token=made-up", Const.FilePermission.WRITE));
        assertFalse(tokenService.validateTokenVal("made-up", "packages/app.apk"));
    }

    @Test
    void enforcesPermissionForIssuedTokens() {
        LocalStorageToken readToken = token("token=read");
        LocalStorageToken writeToken = token("token=write");
        when(storageClient.generateAccessToken(Const.FilePermission.READ)).thenReturn(readToken);
        when(storageClient.generateAccessToken(Const.FilePermission.WRITE)).thenReturn(writeToken);
        when(storageClient.isAccessTokenExpired(readToken)).thenReturn(false);
        when(storageClient.isAccessTokenExpired(writeToken)).thenReturn(false);

        tokenService.generateReadToken("reader");
        tokenService.generateWriteToken("writer");

        assertTrue(tokenService.validateAccessToken("token=read", Const.FilePermission.READ));
        assertFalse(tokenService.validateAccessToken("token=read", Const.FilePermission.WRITE));
        assertTrue(tokenService.validateAccessToken("token=write", Const.FilePermission.WRITE));
    }

    @Test
    void fileTokenOnlyAuthorizesItsCanonicalPath() {
        LocalStorageToken readToken = token("token=file-read");
        when(storageClient.generateAccessTokenForFile(Const.FilePermission.READ, "packages/app.apk"))
                .thenReturn(readToken);
        when(storageClient.isAccessTokenExpired(readToken)).thenReturn(false);

        tokenService.generateReadTokenForFile("reader", "packages/app.apk");

        assertTrue(tokenService.validateTokenVal("file-read", "packages/app.apk"));
        assertTrue(tokenService.validateTokenVal("file-read", "packages/folder/../app.apk"));
        assertFalse(tokenService.validateTokenVal("file-read", "packages/other.apk"));
    }

    @Test
    void rejectsExpiredIssuedToken() {
        LocalStorageToken readToken = token("token=expired");
        when(storageClient.generateAccessToken(Const.FilePermission.READ)).thenReturn(readToken);
        when(storageClient.isAccessTokenExpired(readToken)).thenReturn(true);

        tokenService.generateReadToken("reader");

        assertFalse(tokenService.validateAccessToken("token=expired", Const.FilePermission.READ));
    }

    @Test
    void configuredCenterTokenIsHeaderOnly() {
        assertTrue(tokenService.validateAccessToken("token=center-secret", Const.FilePermission.WRITE));
        assertTrue(tokenService.validateAccessToken("token=center-secret", Const.FilePermission.READ));
        assertFalse(tokenService.validateTokenVal("center-secret", "packages/app.apk"));
    }

    @Test
    void writeTokenRemainsStableForAgentsThatCannotRefreshIt() {
        LocalStorageToken writeToken = token("token=agent-write");
        when(storageClient.generateAccessToken(Const.FilePermission.WRITE)).thenReturn(writeToken);
        when(storageClient.isAccessTokenExpired(writeToken)).thenReturn(true);

        assertSame(tokenService.generateWriteToken("agent"), tokenService.generateWriteToken("agent"));
        assertTrue(tokenService.validateAccessToken("token=agent-write", Const.FilePermission.WRITE));
    }

    private LocalStorageToken token(String value) {
        LocalStorageToken token = new LocalStorageToken();
        token.setToken(value);
        return token;
    }
}
