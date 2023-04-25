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
import java.util.UUID;

public class LocalStorageClientAdapter extends StorageServiceClient {
    private boolean isInitiated = false;
    private LocalStorageClient localStorageClient;
    Logger classLogger = LoggerFactory.getLogger(StorageServiceClient.class);

    public LocalStorageClientAdapter() {
    }

    public LocalStorageClientAdapter(StorageProperties storageProperties) {
        LocalStorageProperty localStorageProperty = (LocalStorageProperty) storageProperties;
        this.localStorageClient = new LocalStorageClient(localStorageProperty);
        fileLimitDay = localStorageProperty.getFileLimitDay();
        classLogger.info("Init Center local storage client successfully!");
    }

    @Override
    public void updateAccessToken(AccessToken accessToken) {
        if (isInitiated) {
            return;
        }
        if (!(accessToken instanceof LocalStorageToken)) {
            return;
        }

        LocalStorageToken localStorageToken = (LocalStorageToken) accessToken;
        localStorageClient = new LocalStorageClient(localStorageToken);
        isInitiated = true;
        classLogger.info("Init Agent local storage client successfully!");
    }

    @Override
    public AccessToken generateAccessToken(String permissionType) {
        LocalStoragePermission permission = LocalStoragePermission.valueOf(permissionType);

        // todo: generate token with specific permissions (WRITE/READ) and expiry time
        LocalStorageToken localStorageToken = new LocalStorageToken();
        localStorageToken.setEndpoint(localStorageClient.getEndpoint());
        localStorageToken.setToken("token=" + UUID.randomUUID());
        return localStorageToken;
    }

    @Override
    public boolean isAccessTokenExpired(AccessToken accessToken) {
        Assert.isTrue(accessToken instanceof LocalStorageToken, "Current accessToken object: " + accessToken + " is not of LocalStorageToken class!");
        LocalStorageToken localStorageToken = (LocalStorageToken) accessToken;
        Assert.notNull(localStorageToken, "The localStorageToken can't be null!");

        // todo: check if the token is expired
        return true;
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
