package com.microsoft.hydralab.common.file.local;

import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.file.AccessToken;
import com.microsoft.hydralab.common.file.StorageServiceClient;
import com.microsoft.hydralab.common.file.impl.local.LocalStorageClientAdapter;
import com.microsoft.hydralab.common.file.impl.local.LocalStorageProperty;
import com.microsoft.hydralab.common.file.impl.local.client.LocalStorageClient;
import com.microsoft.hydralab.common.test.BaseTest;
import com.microsoft.hydralab.common.util.Const;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.io.File;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocalStorageAdapterTest extends BaseTest {
    String storageEndpoint = "http://localhost:9886";
    StorageServiceClient storageServiceClient;
    File sampleFile = new File("src/test/resources/uitestsample.ipa");
    LocalStorageProperty property = new LocalStorageProperty();
    LocalStorageClient mockLocalStorageClient = Mockito.mock(LocalStorageClient.class);
    RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);

    @BeforeAll
    void initStorageServiceClient() {
        property.setEndpoint(storageEndpoint);
        property.setToken("token=" + System.currentTimeMillis());

        storageServiceClient = new LocalStorageClientAdapter(property);
    }

    @Test
    @Order(1)
    void uploadFile() {
        StorageFileInfo fileInfo = new StorageFileInfo(sampleFile, "test/unit/" + sampleFile.getName(), StorageFileInfo.FileType.APP_FILE, EntityType.APP_FILE_SET);

        fileInfo.setBlobContainer(property.getAppFileContainerName());
        String downloadUrl = storageServiceClient.upload(sampleFile, fileInfo).getBlobUrl();
        logger.info("Upload sample file finished, download url: " + downloadUrl);
        Assertions.assertNotNull(downloadUrl, "Upload File Failed!");
    }

    @Test
    @Order(2)
    void downloadFile() {
        File sampleFileCopy = new File("src/test/resources/uitestsample_1.ipa");
        StorageFileInfo fileInfo = new StorageFileInfo();
        fileInfo.setFileType(StorageFileInfo.FileType.APP_FILE);
        fileInfo.setFileName(sampleFileCopy.getName());
        fileInfo.setFileLen(sampleFileCopy.length());
        fileInfo.setBlobPath("test/unit/" + sampleFile.getName());
        fileInfo.setBlobContainer(property.getAppFileContainerName());
        StorageFileInfo fileInfo1 = storageServiceClient.download(sampleFileCopy, fileInfo);
        logger.info("Download sample file finished, properties: " + fileInfo1);
        Assertions.assertNotNull(fileInfo1, "Download File Failed!");
        Assertions.assertTrue(sampleFileCopy.exists(), "Download File Failed!");
        sampleFileCopy.delete();
    }

    @Test
    @Order(3)
    void testGenerateToken() {
        AccessToken accessToken = storageServiceClient.generateAccessToken(Const.FilePermission.READ);
        logger.info("Generate token finished: " + accessToken);
        Assertions.assertNotNull(accessToken, "Generate token Failed!");
    }
}