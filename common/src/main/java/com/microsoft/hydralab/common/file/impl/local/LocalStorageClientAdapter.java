// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.file.impl.local;

import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.file.AccessToken;
import com.microsoft.hydralab.common.file.StorageProperties;
import com.microsoft.hydralab.common.file.StorageServiceClient;
import com.microsoft.hydralab.common.file.impl.local.client.LocalStorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class LocalStorageClientAdapter extends StorageServiceClient {
    private static final long TOKEN_EXPIRY_MINUTES = 120;
    private LocalStorageClient localStorageClient;
    Logger classLogger = LoggerFactory.getLogger(StorageServiceClient.class);

    public LocalStorageClientAdapter() {
    }

    public LocalStorageClientAdapter(StorageProperties storageProperties) {
        LocalStorageProperty localStorageProperty = (LocalStorageProperty) storageProperties;
        this.localStorageClient = new LocalStorageClient(localStorageProperty);
        fileExpiryDay = localStorageProperty.getFileExpiryDay();
        classLogger.info("Init Center local storage client successfully!");
    }

    @Override
    public void updateAccessToken(AccessToken accessToken) {
        if (!(accessToken instanceof LocalStorageToken)) {
            return;
        }

        LocalStorageToken localStorageToken = (LocalStorageToken) accessToken;
        localStorageClient = new LocalStorageClient(localStorageToken);
        fileExpiryDay = localStorageToken.getFileExpiryDay();
        classLogger.info("Updated Agent local storage client access token successfully!");
    }

    @Override
    public AccessToken generateAccessToken(String permissionType) {
        LocalStoragePermission permission = LocalStoragePermission.valueOf(permissionType);

        LocalStorageToken localStorageToken = new LocalStorageToken();
        localStorageToken.setEndpoint(localStorageClient.getEndpoint());
        localStorageToken.setToken("token=" + UUID.randomUUID());
        localStorageToken.setFileExpiryDay(fileExpiryDay);
        localStorageToken.setPermission(permission.name());
        long expiresAt = permission == LocalStoragePermission.WRITE
                ? Long.MAX_VALUE
                : Instant.now().plus(TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES).getEpochSecond();
        localStorageToken.setExpiresAtEpochSecond(expiresAt);
        return localStorageToken;
    }

    @Override
    public AccessToken generateAccessTokenForFile(String permissionType, String fileUri) {
        LocalStorageToken localStorageToken = (LocalStorageToken) generateAccessToken(permissionType);
        localStorageToken.setFileUri(Paths.get(fileUri).normalize().toString().replace(File.separatorChar, '/'));
        return localStorageToken;
    }

    @Override
    public boolean isAccessTokenExpired(AccessToken accessToken) {
        Assert.isTrue(accessToken instanceof LocalStorageToken, "Current accessToken object: " + accessToken + " is not of LocalStorageToken class!");
        LocalStorageToken localStorageToken = (LocalStorageToken) accessToken;
        Assert.notNull(localStorageToken, "The localStorageToken can't be null!");

        return localStorageToken.getExpiresAtEpochSecond() <= Instant.now().getEpochSecond();
    }

    @Override
    public StorageFileInfo upload(File fileToUpload, StorageFileInfo storageFileInfo) {
        String downloadUrl = localStorageClient.upload(fileToUpload, storageFileInfo);
        setFileUrls(storageFileInfo, downloadUrl);
        return storageFileInfo;
    }

    @Override
    public StorageFileInfo download(File downloadToFile, StorageFileInfo storageFileInfo) {
        localStorageClient.download(downloadToFile, storageFileInfo);
        return storageFileInfo;
    }
}
