// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.file.impl.azure;

import com.azure.core.credential.AzureSasCredential;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.PublicAccessType;
import com.azure.storage.common.sas.AccountSasPermission;
import com.azure.storage.common.sas.AccountSasResourceType;
import com.azure.storage.common.sas.AccountSasService;
import com.azure.storage.common.sas.AccountSasSignatureValues;
import com.google.common.net.MediaType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.file.AccessToken;
import com.microsoft.hydralab.common.file.StorageProperties;
import com.microsoft.hydralab.common.file.StorageServiceClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class AzureBlobClientAdapter extends StorageServiceClient {
    private static boolean isAuthedBySAS = true;
    private BlobServiceClient blobServiceClient;
    Logger classLogger = LoggerFactory.getLogger(AzureBlobClientAdapter.class);
    private long SASExpiryUpdate;
    private SASData sasDataInUse = null;
    private SASData sasDataForUpdate = null;
    private boolean isConnected = false;

    public AzureBlobClientAdapter() {
    }

    public AzureBlobClientAdapter(StorageProperties storageProperties) {
        AzureBlobProperty azureBlobProperty = (AzureBlobProperty) storageProperties;
        SASExpiryUpdate = azureBlobProperty.getSASExpiryUpdate();
        SASPermission.READ.setExpiryTime(azureBlobProperty.getSASExpiryTimeFront(), azureBlobProperty.getTimeUnit());
        SASPermission.WRITE.setExpiryTime(azureBlobProperty.getSASExpiryTimeAgent(), azureBlobProperty.getTimeUnit());
        blobServiceClient = new BlobServiceClientBuilder().connectionString(azureBlobProperty.getConnection()).buildClient();
        fileExpiryDay = azureBlobProperty.getFileExpiryDay();
        cdnUrl = azureBlobProperty.getCDNUrl();
        isAuthedBySAS = false;
        isConnected = true;
    }

    @Override
    public void updateAccessToken(AccessToken accessToken) {
        if (!(accessToken instanceof SASData)) {
            return;
        }
        SASData sasData = (SASData) accessToken;

        if (sasDataInUse == null) {
            buildClientBySAS(sasData);
        } else if (!StringUtils.isEmpty(sasData.getToken()) && !sasDataInUse.getToken().equals(sasData.getToken())) {
            sasDataForUpdate = sasData;
        }
    }

    @Override
    public AccessToken generateAccessToken(String permissionType) {
        SASPermission permission = SASPermission.valueOf(permissionType);

        return generateSAS(permission);
    }

    @Override
    public boolean isAccessTokenExpired(AccessToken accessToken) {
        Assert.isTrue(accessToken instanceof SASData, "Current accessToken object: " + accessToken + " is not of SASData class!");
        SASData sasData = (SASData) accessToken;
        Assert.notNull(sasData, "The sasData can't be null!");

        return sasData.getExpiredTime().isBefore(OffsetDateTime.ofInstant(Instant.now().plus(SASExpiryUpdate, ChronoUnit.MINUTES), ZoneId.systemDefault()));
    }

    @Override
    public StorageFileInfo upload(File fileToUpload, StorageFileInfo fileInfo) {
        String downloadUrl = uploadFileToBlob(fileToUpload, fileInfo.getBlobContainer(), fileInfo.getBlobPath(), null);
        setFileUrls(fileInfo, downloadUrl);
        return fileInfo;
    }

    @Override
    public StorageFileInfo download(File downloadToFile, StorageFileInfo fileInfo) {
        downloadFileFromBlob(downloadToFile, fileInfo.getBlobContainer(), fileInfo.getBlobPath());
        return fileInfo;
    }

    private void buildClientBySAS(SASData sasData) {
        AzureSasCredential azureSasCredential = new AzureSasCredential(sasData.getToken());
        blobServiceClient = new BlobServiceClientBuilder().endpoint(sasData.getEndpoint()).credential(azureSasCredential).buildClient();
        fileExpiryDay = sasData.getFileExpiryDay();
        cdnUrl = sasData.getCdnUrl();
        isConnected = true;
        sasDataInUse = sasData;
    }

    private void checkBlobClientUpdate() {
        if (isAuthedBySAS && sasDataForUpdate != null) {
            buildClientBySAS(sasDataForUpdate);
            sasDataForUpdate = null;
        }
    }

    private BlobContainerClient getContainer(String containerName) {
        BlobContainerClient blobContainerClient;
        try {
            blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
            classLogger.info("Get a BlobContainerClient for container {}", containerName);
            if (!blobContainerClient.exists()) {
                classLogger.info("Container {} doesn't exist, will try to create it.", containerName);
                blobContainerClient.create();
            }
        } catch (BlobStorageException e) {
            classLogger.info("Can't connect to container for {}. Try to create one!", containerName);
            blobContainerClient = blobServiceClient.createBlobContainerWithResponse(containerName, null, PublicAccessType.CONTAINER, Context.NONE).getValue();
        }
        return blobContainerClient;
    }

    public SASData generateSAS(SASPermission sasPermission) {
        Assert.isTrue(!isAuthedBySAS, "The client was init by SAS and can't generate SAS!");

        SASData sasData = new SASData();
        AccountSasService services = AccountSasService.parse(sasPermission.serviceStr);
        AccountSasResourceType resourceTypes = AccountSasResourceType.parse(sasPermission.resourceStr);
        AccountSasPermission permissions = AccountSasPermission.parse(sasPermission.permissionStr);
        OffsetDateTime expiryTime = OffsetDateTime.ofInstant(Instant.now().plus(sasPermission.expiryTime, sasPermission.timeUnit), ZoneId.systemDefault());

        AccountSasSignatureValues sasSignatureValues = new AccountSasSignatureValues(expiryTime, permissions,
                services, resourceTypes);

        sasData.setToken(blobServiceClient.generateAccountSas(sasSignatureValues));
        sasData.setExpiredTime(expiryTime);
        sasData.setEndpoint(blobServiceClient.getAccountUrl());
        sasData.setSasPermission(sasPermission);
        sasData.setFileExpiryDay(fileExpiryDay);
        sasData.setCdnUrl(cdnUrl);
        return sasData;
    }

    /**
     * Upload a file to the blob container. If the file already exists, overwrite it.
     *
     * @param uploadFile
     * @param containerName
     * @param blobFilePath
     * @param logger
     * @return
     */
    public String uploadFileToBlob(File uploadFile, String containerName, String blobFilePath, Logger logger) {
        if (!isConnected) {
            return null;
        }
        checkBlobClientUpdate();
        if (logger == null) {
            logger = classLogger;
        }

        BlobClient blobClient = getContainer(containerName).getBlobClient(blobFilePath);
        if (uploadFile.getName().endsWith(MediaType.MP4_VIDEO.subtype())) {
            BlobHttpHeaders headers = new BlobHttpHeaders();
            headers.setContentType(MediaType.MP4_VIDEO.toString());
            blobClient.uploadFromFile(uploadFile.getAbsolutePath(), null, headers, null, null, null, null);
        } else {
            blobClient.uploadFromFile(uploadFile.getAbsolutePath(), true);
        }
        logger.info("upload file {} to Blob, Size {}", uploadFile.getName(), uploadFile.length());
        return blobClient.getBlobUrl();
    }

    /**
     * Download a file from the blob container. If the file already exists, overwrite it.
     *
     * @param downloadToFile
     * @param containerName
     * @param blobFilePath
     * @return
     */
    public BlobProperties downloadFileFromBlob(File downloadToFile, String containerName, String blobFilePath) {
        if (!isConnected) {
            return null;
        }
        checkBlobClientUpdate();
        File saveDir = downloadToFile.getParentFile();
        if (!saveDir.exists()) {
            Assert.isTrue(saveDir.mkdirs(), "mkdirs fail in downloadFileFromUrl");
        }
        BlobClient blobClient = getContainer(containerName).getBlobClient(blobFilePath);
        return blobClient.downloadToFile(downloadToFile.getAbsolutePath(), true);
    }
}
