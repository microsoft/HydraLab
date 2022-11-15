// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util.blob;

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
import com.microsoft.hydralab.common.entity.common.EntityFileRelation.EntityType;
import com.microsoft.hydralab.common.entity.common.SASData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class BlobStorageClient {
    private static boolean initTag = false;
    private static boolean isAuthedBySAS = true;
    private BlobServiceClient blobServiceClient;
    Logger classLogger = LoggerFactory.getLogger(BlobStorageClient.class);
    private long SASExpiryTimeFont;
    private long SASExpiryTimeAgent;
    private long SASExpiryUpdate;
    private SASData sasDataInUse = null;
    private SASData sasDataForUpdate = null;

    public BlobStorageClient() {
    }

    public BlobStorageClient(String connectionStr, long SASExpiryTimeFont, long SASExpiryTimeAgent, long SASExpiryUpdate) {
        this.SASExpiryTimeFont = SASExpiryTimeFont;
        this.SASExpiryTimeAgent = SASExpiryTimeAgent;
        this.SASExpiryUpdate = SASExpiryUpdate;
        blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionStr).buildClient();
        initContainer();
        initTag = true;
        isAuthedBySAS = false;
    }

    public void setSASData(SASData sasData) {
        if (!initTag && sasDataInUse == null) {
            buildClientBySAS(sasData);
            initContainer();
            initTag = true;
        } else if (!StringUtils.isEmpty(sasData.getSignature()) && !sasDataInUse.getSignature().equals(sasData.getSignature())) {
            sasDataForUpdate = sasData;
        }
    }

    private void buildClientBySAS(SASData sasData) {
        AzureSasCredential azureSasCredential = new AzureSasCredential(sasData.getSignature());
        blobServiceClient = new BlobServiceClientBuilder().endpoint(sasData.getEndpoint()).credential(azureSasCredential).buildClient();
        sasDataInUse = sasData;
    }

    private void checkBlobStorageClient() {
        if (isAuthedBySAS && sasDataForUpdate != null) {
            buildClientBySAS(sasDataForUpdate);
            sasDataForUpdate = null;
        }
    }

    private void initContainer() {
        EntityType[] entityTypes = EntityType.values();
        for (EntityType entityType : entityTypes) {
            String containerName = entityType.blobConstant;
            try {
                blobServiceClient.getBlobContainerClient(containerName);
                classLogger.info("Get a BlobContainerClient for container {}", containerName);
            } catch (BlobStorageException e) {
                classLogger.info("Can't connect to container for {}. Try to create one!", containerName);
                blobServiceClient.createBlobContainerWithResponse(containerName, null, PublicAccessType.CONTAINER, Context.NONE);
            }
        }
    }

    public SASData generateSAS(String serviceStr, String resourceStr, String permissionStr) {
        Assert.isTrue(!isAuthedBySAS, "The client was init by SAS and can't generate SAS!");

        SASData sasData = new SASData();
        AccountSasService services = AccountSasService.parse(serviceStr);
        AccountSasResourceType resourceTypes = AccountSasResourceType.parse(resourceStr);
        AccountSasPermission permissions = AccountSasPermission.parse(permissionStr);
        OffsetDateTime expiryTime = OffsetDateTime.ofInstant(Instant.now().plus(SASExpiryTimeFont, ChronoUnit.MINUTES), ZoneId.systemDefault());

        AccountSasSignatureValues sasSignatureValues = new AccountSasSignatureValues(expiryTime, permissions,
                services, resourceTypes);

        sasData.setSignature(blobServiceClient.generateAccountSas(sasSignatureValues));
        sasData.setExpiredTime(expiryTime);
        sasData.setEndpoint(blobServiceClient.getAccountUrl());

        return sasData;
    }

    public Boolean isSASExpired(SASData sasData) {
        Assert.notNull(sasData, "The sasData can't be null!");
        return sasData.getExpiredTime().isBefore(OffsetDateTime.ofInstant(Instant.now().plus(SASExpiryUpdate, ChronoUnit.MINUTES), ZoneId.systemDefault()));
    }

    /**
     * Upload a file to the blob container. If the file already exists, overwrite it.
     *
     * @param uploadFile
     * @param blobFilePath
     * @param containerName
     * @return
     */
    public String uploadBlobFromFile(File uploadFile, String containerName, String blobFilePath, Logger logger) {
        checkBlobStorageClient();
        if (logger == null) {
            logger = classLogger;
        }
        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        logger.info("Get a BlobContainerClient for container {} for file {}", containerName, uploadFile.getAbsoluteFile());

        BlobClient blobClient = blobContainerClient.getBlobClient(blobFilePath);
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
        checkBlobStorageClient();
        File saveDir = downloadToFile.getParentFile();
        if (!saveDir.exists()) {
            cn.hutool.core.lang.Assert.isTrue(saveDir.mkdirs(), "mkdirs fail in downloadFileFromUrl");
        }
        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = blobContainerClient.getBlobClient(blobFilePath);
        return blobClient.downloadToFile(downloadToFile.getAbsolutePath(), true);
    }
}
