package com.microsoft.hydralab.common.util;

import com.microsoft.hydralab.common.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FileUtilTest extends BaseTest {

    @Test
    void isLegalFolderPath() {
        String legalFilePath = "test/aaa";
        String illegalFilePath = "/test";
        String illegalFilePath1 = "test/../../../aaa";
        logger.info("Verify legal path: " + legalFilePath);
        Assertions.assertTrue(FileUtil.isLegalFolderPath(legalFilePath), "Verify folder error!");
        logger.info("Verify illegal path: " + illegalFilePath);
        Assertions.assertFalse(FileUtil.isLegalFolderPath(illegalFilePath), "Verify folder error!");
        logger.info("Verify illegal path: " + illegalFilePath1);
        Assertions.assertFalse(FileUtil.isLegalFolderPath(illegalFilePath1), "Verify folder error!");
    }

    @Test
    void getLegalFileName() {
        String legalFileName = "test.json";
        String illegalFileName = "te.st.json";
        logger.info("Format legal fileName: " + legalFileName);
        Assertions.assertTrue(legalFileName.equals(FileUtil.getLegalFileName(legalFileName)), "Format fileName Error!");
        logger.info("Format illegal fileName: " + illegalFileName);
        Assertions.assertTrue(legalFileName.equals(FileUtil.getLegalFileName(illegalFileName)), "Format fileName Error!");
    }
}