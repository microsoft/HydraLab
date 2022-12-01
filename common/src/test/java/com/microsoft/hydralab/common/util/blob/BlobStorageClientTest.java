package com.microsoft.hydralab.common.util.blob;

import com.azure.storage.blob.models.BlobProperties;
import com.microsoft.hydralab.common.entity.center.BlobProperty;
import com.microsoft.hydralab.common.entity.common.SASData;
import com.microsoft.hydralab.common.test.BaseTest;
import org.junit.jupiter.api.*;
import org.junit.platform.commons.util.StringUtils;

import java.io.File;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlobStorageClientTest extends BaseTest {
    String connectionString = "";
    BlobStorageClient blobStorageClient;
    File sampleFile = new File("src/test/resources/uitestsample.ipa");
    BlobProperty property = new BlobProperty();

    @BeforeAll
    void initBlob() {
        property.setConnection(connectionString);
        property.setFileLimitDay(6);
        property.setSASExpiryTimeAgent(30);
        property.setSASExpiryTimeFront(5);
        property.setSASExpiryUpdate(0);
        property.setTimeUnit("SECONDS");

        if (StringUtils.isBlank(connectionString)) {
            blobStorageClient = new MockBlobStorageClient(property);
        } else {
            blobStorageClient = new BlobStorageClient(property);
        }
    }

    @Test
    @Order(1)
    void uploadBlobFromFile() {
        String blobUrl = blobStorageClient.uploadBlobFromFile(sampleFile, DeviceNetworkBlobConstants.PKG_BLOB_NAME, "test/unit/" + sampleFile.getName(), logger);
        logger.info("Upload sample file finished, blobUrl: " + blobUrl);
        Assertions.assertNotNull(blobUrl, "Upload File Failed!");
    }

    @Test
    @Order(2)
    void downloadFileFromBlob() {
        if (!(blobStorageClient instanceof MockBlobStorageClient)) {
            File sampleFile_copy = new File("src/test/resources/uitestsample_1.ipa");
            BlobProperties properties = blobStorageClient.downloadFileFromBlob(sampleFile_copy, DeviceNetworkBlobConstants.PKG_BLOB_NAME, "test/unit/" + sampleFile.getName());
            logger.info("Download sample file finished, properties: " + properties);
            Assertions.assertNotNull(properties, "Download File Failed!");
            if (sampleFile_copy.exists()) {
                sampleFile_copy.delete();
            }
        }
    }

    @Test
    @Order(3)
    void testGenerateSAS() {
        SASData sasdata = blobStorageClient.generateSAS(SASData.SASPermission.Read);
        logger.info("Generate SAS finished: " + sasdata);
        Assertions.assertNotNull(sasdata, "Download File Failed!");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assertions.assertTrue(blobStorageClient.isSASExpired(sasData), "Check SAS expired fail!");
    }
}