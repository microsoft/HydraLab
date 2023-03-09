// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.file.impl.local;

import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.file.AccessToken;
import com.microsoft.hydralab.common.file.StorageProperties;
import com.microsoft.hydralab.common.file.StorageServiceClient;
import com.microsoft.hydralab.common.file.impl.local.client.LocalStorageClient;
import org.springframework.util.Assert;

import java.io.File;

public class LocalStorageClientAdapter extends StorageServiceClient {
    private boolean isInitiated = false;
    private final LocalStorageClient localStorageClient;

    public LocalStorageClientAdapter() {
        this.localStorageClient = new LocalStorageClient();
    }

    public LocalStorageClientAdapter(StorageProperties storageProperties) {
        LocalStorageProperty localStorageProperty = (LocalStorageProperty) storageProperties;
        this.localStorageClient = new LocalStorageClient(localStorageProperty);
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
        localStorageClient.init(localStorageToken);
        isInitiated = true;
    }

    @Override
    public AccessToken generateAccessToken(String permissionType) {
        LocalStoragePermission permission = LocalStoragePermission.valueOf(permissionType);

        // todo: generate token with specific permissions and expiry time
        LocalStorageToken localStorageToken = new LocalStorageToken();
        localStorageToken.setToken("anytoken");
        localStorageToken.setEndpoint(localStorageClient.getEndpoint());
        return localStorageToken;
//        return null;
//        return generateJWT(permission);
    }

    @Override
    public boolean isAccessTokenExpired(AccessToken accessToken) {
        Assert.isTrue(accessToken instanceof LocalStorageToken, "Current accessToken object: " + accessToken + " is not of LocalStorageToken class!");
        LocalStorageToken localStorageToken = (LocalStorageToken) accessToken;
        Assert.notNull(localStorageToken, "The localStorageToken can't be null!");

        // todo: check if the token is expired
        return true;
//        return false;
//        return sasData.getExpiredTime().isBefore(OffsetDateTime.ofInstant(Instant.now().plus(SASExpiryUpdate, ChronoUnit.MINUTES), ZoneId.systemDefault()));
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

//    private void buildClientBySAS(SASData sasData) {
//        AzureSasCredential azureSasCredential = new AzureSasCredential(sasData.getToken());
//        blobServiceClient = new BlobServiceClientBuilder().endpoint(sasData.getEndpoint()).credential(azureSasCredential).buildClient();
//        fileLimitDay = sasData.getFileLimitDay();
//        cdnUrl = sasData.getCdnUrl();
//        isConnected = true;
//        sasDataInUse = sasData;
//    }
//
//    private void checkBlobClientUpdate() {
//        if (isAuthedBySAS && sasDataForUpdate != null) {
//            buildClientBySAS(sasDataForUpdate);
//            sasDataForUpdate = null;
//        }
//    }
//
//    private BlobContainerClient getContainer(String containerName) {
//        BlobContainerClient blobContainerClient;
//        try {
//            blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
//            classLogger.info("Get a BlobContainerClient for container {}", containerName);
//            if (!blobContainerClient.exists()) {
//                classLogger.info("Container {} doesn't exist, will try to create it.", containerName);
//                blobContainerClient.create();
//            }
//        } catch (BlobStorageException e) {
//            classLogger.info("Can't connect to container for {}. Try to create one!", containerName);
//            blobContainerClient = blobServiceClient.createBlobContainerWithResponse(containerName, null, PublicAccessType.CONTAINER, Context.NONE).getValue();
//        }
//        return blobContainerClient;
//    }
//
//    public SASData generateSAS(SASPermission sasPermission) {
//        Assert.isTrue(!isAuthedBySAS, "The client was init by SAS and can't generate SAS!");
//
//        SASData sasData = new SASData();
//        AccountSasService services = AccountSasService.parse(sasPermission.serviceStr);
//        AccountSasResourceType resourceTypes = AccountSasResourceType.parse(sasPermission.resourceStr);
//        AccountSasPermission permissions = AccountSasPermission.parse(sasPermission.permissionStr);
//        OffsetDateTime expiryTime = OffsetDateTime.ofInstant(Instant.now().plus(sasPermission.expiryTime, sasPermission.timeUnit), ZoneId.systemDefault());
//
//        AccountSasSignatureValues sasSignatureValues = new AccountSasSignatureValues(expiryTime, permissions,
//                services, resourceTypes);
//
//        sasData.setToken(blobServiceClient.generateAccountSas(sasSignatureValues));
//        sasData.setExpiredTime(expiryTime);
//        sasData.setEndpoint(blobServiceClient.getAccountUrl());
//        sasData.setSasPermission(sasPermission);
//        sasData.setFileLimitDay(fileLimitDay);
//        sasData.setCdnUrl(cdnUrl);
//        return sasData;
//    }
}
