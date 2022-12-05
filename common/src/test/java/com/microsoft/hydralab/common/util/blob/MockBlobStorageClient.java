// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util.blob;

import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.common.sas.AccountSasPermission;
import com.azure.storage.common.sas.AccountSasResourceType;
import com.azure.storage.common.sas.AccountSasService;
import com.azure.storage.common.sas.AccountSasSignatureValues;
import com.microsoft.hydralab.common.entity.center.BlobProperty;
import com.microsoft.hydralab.common.entity.common.SASData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * @author zhoule
 * @date 11/30/2022
 */

public class MockBlobStorageClient extends BlobStorageClient {
    private static boolean isAuthedBySAS = true;
    private static boolean isConnected = true;
    Logger classLogger = LoggerFactory.getLogger(MockBlobStorageClient.class);
    private long SASExpiryUpdate;

    public MockBlobStorageClient(BlobProperty blobProperty) {
        this.SASExpiryUpdate = blobProperty.getSASExpiryUpdate();
        SASData.SASPermission.Read.setExpiryTime(blobProperty.getSASExpiryTimeFront(), blobProperty.getTimeUnit());
        SASData.SASPermission.Write.setExpiryTime(blobProperty.getSASExpiryTimeAgent(), blobProperty.getTimeUnit());
        fileLimitDay = blobProperty.getFileLimitDay();
        cdnUrl = blobProperty.getCDNUrl();
        isAuthedBySAS = false;
        isConnected = true;
        classLogger.info("Init blob client successfully!");
    }

    @Override
    public void setSASData(SASData sasData) {

    }


    @Override
    public SASData generateSAS(SASData.SASPermission sasPermission) {
        Assert.isTrue(!isAuthedBySAS, "The client was init by SAS and can't generate SAS!");

        SASData sasData = new SASData();
        OffsetDateTime expiryTime = OffsetDateTime.ofInstant(Instant.now().plus(sasPermission.expiryTime, sasPermission.timeUnit), ZoneId.systemDefault());

        sasData.setSignature("test");
        sasData.setExpiredTime(expiryTime);
        sasData.setEndpoint("");
        sasData.setSasPermission(sasPermission);
        sasData.setFileLimitDay(fileLimitDay);
        sasData.setCdnUrl(cdnUrl);
        return sasData;
    }

    @Override
    public String uploadBlobFromFile(File uploadFile, String containerName, String blobFilePath, Logger logger) {
        classLogger.info("Upload blob client successfully!");
        return "blobUrl";
    }
}
