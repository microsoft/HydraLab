// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util.blob;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.PublicAccessType;
import com.google.common.net.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class BlobStorageClient {
    private final BlobServiceClient blobServiceClient;
    Logger classlogger = LoggerFactory.getLogger(BlobStorageClient.class);

    public BlobStorageClient(String connectionStr) {
        blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionStr).buildClient();
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
        if (logger == null) {
            logger = classlogger;
        }
        BlobContainerClient blobContainerClient;
        try {
            blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
            logger.info("Get a BlobContainerClient for container {} for file {}", containerName, uploadFile.getAbsoluteFile());
        } catch (BlobStorageException e) {
            logger.info("Can't connect to container for {}. Try to create one!", containerName);
            blobServiceClient.createBlobContainerWithResponse(containerName, null, PublicAccessType.CONTAINER, Context.NONE);
            blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        }

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
    public boolean downloadFileFromBlob(File downloadToFile, String containerName, String blobFilePath) {
        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = blobContainerClient.getBlobClient(blobFilePath);
        blobClient.downloadToFile(downloadToFile.getAbsolutePath(), true);
        return true;
    }
}
