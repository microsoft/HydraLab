package com.microsoft.hydralab.common.file.azure;

import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.file.AccessToken;
import com.microsoft.hydralab.common.file.StorageServiceClient;
import com.microsoft.hydralab.common.file.impl.azure.AzureBlobClientAdapter;
import com.microsoft.hydralab.common.file.impl.azure.AzureBlobProperty;
import com.microsoft.hydralab.common.test.BaseTest;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.ThreadUtils;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.platform.commons.util.StringUtils;

import java.io.File;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlobClientTest extends BaseTest {
    StorageServiceClient storageServiceClient;
    File sampleFile = new File("src/test/resources/uitestsample.ipa");
    AzureBlobProperty property = new AzureBlobProperty();

    @BeforeAll
    void initBlob() {
        String connectionString = null;
        try {
            Dotenv dotenv = Dotenv.load();
            connectionString = dotenv.get("BLOB_CONNECTION_STRING");
            logger.info("Get connectionString from env file successfully!");
        } catch (Exception e) {
            logger.error("Get connectionString from env file failed!", e);
        }

        property.setConnection(connectionString);
        property.setFileExpiryDay(6);
        property.setSASExpiryTimeAgent(30);
        property.setSASExpiryTimeFront(5);
        property.setSASExpiryUpdate(0);
        property.setTimeUnit("SECONDS");

        if (StringUtils.isBlank(connectionString)) {
            storageServiceClient = new MockAzureBlobClient(property);
        } else {
            storageServiceClient = new AzureBlobClientAdapter(property);
        }
    }

    @Test
    @Order(1)
    void uploadFile() {
        StorageFileInfo fileInfo = new StorageFileInfo(sampleFile, "test/unit/" + sampleFile.getName(), StorageFileInfo.FileType.APP_FILE, EntityType.APP_FILE_SET);
        fileInfo.setBlobContainer(property.getAppFileContainerName());
        String downloadUrl = storageServiceClient.upload(sampleFile, fileInfo).getBlobUrl();
        logger.info("Upload sample file finished, blobUrl: " + downloadUrl);
        Assertions.assertNotNull(downloadUrl, "Upload File Failed!");
    }

    @Test
    @Order(2)
    void downloadFile() {
        if (!(storageServiceClient instanceof MockAzureBlobClient)) {
            File sampleFile_copy = new File("src/test/resources/uitestsample_1.ipa");
            StorageFileInfo fileInfo = new StorageFileInfo();
            fileInfo.setFileType(StorageFileInfo.FileType.APP_FILE);
            fileInfo.setFileName(sampleFile_copy.getName());
            fileInfo.setFileLen(sampleFile_copy.length());
            fileInfo.setBlobPath("test/unit/" + sampleFile.getName());
            fileInfo.setBlobContainer(property.getAppFileContainerName());
            StorageFileInfo properties = storageServiceClient.download(sampleFile_copy, fileInfo);
            logger.info("Download sample file finished, properties: " + properties);
            Assertions.assertNotNull(properties, "Download File Failed!");
            Assertions.assertTrue(sampleFile_copy.exists(), "Download File Failed!");
            sampleFile_copy.delete();
        }
    }

    @Test
    @Order(3)
    void testGenerateSAS() {
        AccessToken accessToken = storageServiceClient.generateAccessToken(Const.FilePermission.READ);
        logger.info("Generate SAS finished: " + accessToken);
        Assertions.assertNotNull(accessToken, "Generate SAS Failed!");
        ThreadUtils.safeSleep(5000);
        Assertions.assertTrue(storageServiceClient.isAccessTokenExpired(accessToken), "Check SAS expired fail!");
    }
}