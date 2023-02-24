package com.microsoft.hydralab.common.file.blob;

import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.file.AccessToken;
import com.microsoft.hydralab.common.file.StorageServiceClient;
import com.microsoft.hydralab.common.file.impl.blob.*;
import com.microsoft.hydralab.common.test.BaseTest;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.ThreadUtils;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.*;
import org.junit.platform.commons.util.StringUtils;

import java.io.File;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlobClientTest extends BaseTest {
    StorageServiceClient storageServiceClient;
    File sampleFile = new File("src/test/resources/uitestsample.ipa");
    BlobProperty property = new BlobProperty();

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
        property.setFileLimitDay(6);
        property.setSASExpiryTimeAgent(30);
        property.setSASExpiryTimeFront(5);
        property.setSASExpiryUpdate(0);
        property.setTimeUnit("SECONDS");

        if (StringUtils.isBlank(connectionString)) {
            storageServiceClient = new MockBlobClient(property);
        } else {
            storageServiceClient = new BlobClientAdapter(property);
        }
    }

    @Test
    @Order(1)
    void uploadFile() {
        StorageFileInfo fileInfo = new StorageFileInfo(sampleFile, "test/unit/" + sampleFile.getName(), StorageFileInfo.FileType.APP_FILE, EntityType.APP_FILE_SET);
        String downloadUrl = storageServiceClient.upload(sampleFile, fileInfo).getFileDownloadUrl();
        logger.info("Upload sample file finished, blobUrl: " + downloadUrl);
        Assertions.assertNotNull(downloadUrl, "Upload File Failed!");
    }

    @Test
    @Order(2)
    void downloadFile() {
        if (!(storageServiceClient instanceof MockBlobClient)) {
            File sampleFile_copy = new File("src/test/resources/uitestsample_1.ipa");
            StorageFileInfo fileInfo = new StorageFileInfo();
            fileInfo.setFileType(StorageFileInfo.FileType.APP_FILE);
            fileInfo.setFileName(sampleFile_copy.getName());
            fileInfo.setFileLen(sampleFile_copy.length());
            fileInfo.setFileRelPath("test/unit/" + sampleFile.getName());
            fileInfo.setStorageContainer(EntityType.APP_FILE_SET.storageContainer);
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