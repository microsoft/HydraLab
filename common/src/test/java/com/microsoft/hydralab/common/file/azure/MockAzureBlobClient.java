// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.file.azure;

import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.file.impl.azure.AzureBlobProperty;
import com.microsoft.hydralab.common.file.impl.azure.AzureBlobClientAdapter;
import com.microsoft.hydralab.common.file.impl.azure.SASData;
import com.microsoft.hydralab.common.file.impl.azure.SASPermission;
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

public class MockAzureBlobClient extends AzureBlobClientAdapter {
    private static boolean isAuthedBySAS = true;
    private static boolean isConnected = true;
    Logger classLogger = LoggerFactory.getLogger(MockAzureBlobClient.class);
    private long SASExpiryUpdate;

    public MockAzureBlobClient(AzureBlobProperty azureBlobProperty) {
        this.SASExpiryUpdate = azureBlobProperty.getSASExpiryUpdate();
        SASPermission.READ.setExpiryTime(azureBlobProperty.getSASExpiryTimeFront(), azureBlobProperty.getTimeUnit());
        SASPermission.WRITE.setExpiryTime(azureBlobProperty.getSASExpiryTimeAgent(), azureBlobProperty.getTimeUnit());
        fileLimitDay = azureBlobProperty.getFileLimitDay();
        cdnUrl = azureBlobProperty.getCDNUrl();
        isAuthedBySAS = false;
        isConnected = true;
        classLogger.info("Init blob client successfully!");
    }

    public SASData generateSAS(SASPermission sasPermission) {
        Assert.isTrue(!isAuthedBySAS, "The client was init by SAS and can't generate SAS!");

        SASData sasData = new SASData();
        OffsetDateTime expiryTime = OffsetDateTime.ofInstant(Instant.now().plus(sasPermission.expiryTime, sasPermission.timeUnit), ZoneId.systemDefault());

        sasData.setToken("test");
        sasData.setExpiredTime(expiryTime);
        sasData.setEndpoint("");
        sasData.setSasPermission(sasPermission);
        sasData.setFileLimitDay(fileLimitDay);
        sasData.setCdnUrl(cdnUrl);
        return sasData;
    }

    @Override
    public StorageFileInfo upload(File uploadFile, StorageFileInfo fileInfo) {
        fileInfo.setBlobUrl("downloadUrl");
        classLogger.info("Upload blob client successfully!");
        return fileInfo;
    }
}
