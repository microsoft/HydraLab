package com.microsoft.hydralab.common.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.microsoft.hydralab.common.entity.common.BlobFileInfo;
import com.microsoft.hydralab.common.entity.common.EntityFileRelation;
import net.dongliu.apk.parser.ApkFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PkgUtilTest {
    Logger logger = LoggerFactory.getLogger(PkgUtilTest.class);

    @Test
    public void analysisApkFile() throws IOException {
        File recordFile = new File("src/main/resources/record_release.apk");
        JSONObject res = PkgUtil.analysisFile(recordFile, EntityFileRelation.EntityType.APP_FILE_SET);

        logger.info(res.toString(SerializerFeature.PrettyFormat));
        Assertions.assertTrue("com.microsoft.hydralab.android.client".equals(res.getString(BlobFileInfo.ParserKey.PkgName)), "Analysis apk error!");

        try (ApkFile apkFile = new ApkFile(recordFile)) {
            List<String> usesPermissions = apkFile.getApkMeta().getUsesPermissions();
            for (String usesPermission : usesPermissions) {
                logger.info(usesPermission);
            }
        }
    }

    @Test
    public void analysisIpaFile() {
        File recordFile = new File("src/test/resources/uitestsample.ipa");
        JSONObject res = PkgUtil.analysisFile(recordFile, EntityFileRelation.EntityType.APP_FILE_SET);

        logger.info(res.toString(SerializerFeature.PrettyFormat));
        Assertions.assertTrue("com.microsoft.es.uitestsample".equals(res.getString(BlobFileInfo.ParserKey.PkgName)), "Analysis ipa error!");
    }
}