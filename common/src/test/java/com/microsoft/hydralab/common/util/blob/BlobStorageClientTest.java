package com.microsoft.hydralab.common.util.blob;

import com.microsoft.hydralab.common.entity.common.SASData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

class BlobStorageClientTest {

    BlobStorageClient blobStorageClient = new BlobStorageClient();
    SASData sasData = new SASData() {{
        setEndpoint("test");
        setSignature("aaa");
        setExpiredTime(OffsetDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
    }};

    @Test
    void setSASData() {
        SASData sasData = new SASData();
        sasData.setEndpoint("test");
        sasData.setSignature("aaa");
        try {
            blobStorageClient.setSASData(sasData);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }

    }

    @Test
    void generateSAS() {
        try {
            blobStorageClient.generateSAS(SASData.SASPermission.Read);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }

    }

    @Test
    void isSASExpired() {
        // Wait 2s to make the sas expired.
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assertions.assertTrue(blobStorageClient.isSASExpired(sasData), "Check SAS expired fail!");
    }
}