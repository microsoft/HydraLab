package com.microsoft.hydralab.common.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.test.BaseTest;
import net.dongliu.apk.parser.ApkFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PkgUtilTest extends BaseTest {

    @Test
    public void analysisApkFile() throws IOException {
        File recordFile = new File("src/main/resources/record_release.apk");
        JSONObject res = PkgUtil.analysisFile(recordFile, EntityType.APP_FILE_SET);

        logger.info(res.toString(SerializerFeature.PrettyFormat));
        Assertions.assertTrue("com.microsoft.hydralab.android.client".equals(res.getString(StorageFileInfo.ParserKey.PKG_NAME)), "Analysis apk error!");

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
        JSONObject res = PkgUtil.analysisFile(recordFile, EntityType.APP_FILE_SET);

        logger.info(res.toString(SerializerFeature.PrettyFormat));
        Assertions.assertTrue("com.microsoft.es.uitestsample".equals(res.getString(StorageFileInfo.ParserKey.PKG_NAME)), "Analysis ipa error!");
    }
}
