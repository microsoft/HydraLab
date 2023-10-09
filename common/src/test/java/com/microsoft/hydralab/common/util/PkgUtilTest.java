package com.microsoft.hydralab.common.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.entity.common.TestAppContext;
import com.microsoft.hydralab.common.test.BaseTest;
import net.dongliu.apk.parser.ApkFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PkgUtilTest extends BaseTest {

    @Test
    public void analysisApkFile() throws IOException {
        File recordFile = new File("src/test/resources/record_release.apk");
        JSONObject res = PkgUtil.analysisFile(recordFile, EntityType.APP_FILE_SET);

        logger.info(res.toString(SerializerFeature.PrettyFormat));
        Assertions.assertEquals("com.microsoft.hydralab.android.client", res.getString(StorageFileInfo.ParserKey.PKG_NAME), "Analysis apk error!");

        try (ApkFile apkFile = new ApkFile(recordFile)) {
            List<String> usesPermissions = apkFile.getApkMeta().getUsesPermissions();
            for (String usesPermission : usesPermissions) {
                logger.info(usesPermission);
            }
        }
    }

    @Test
    public void analysisApkManifest() throws IOException {
        File apkFile = new File("src/test/resources/record_release.apk");

        String manifestXml = PkgUtil.getAndroidPackageManifest(apkFile);
        logger.info("Full manifest: {}", manifestXml);

        List<String> androidPackageActivities = PkgUtil.getAndroidPackageActivities(apkFile);
        for (String androidPackageActivity : androidPackageActivities) {
            logger.info("Find Activity: {}", androidPackageActivity);
        }
        Assertions.assertFalse(androidPackageActivities.isEmpty(), "androidPackageActivities is empty!");

        List<String> androidPackageComponents = PkgUtil.getAndroidPackageComponents(apkFile);
        for (String component : androidPackageComponents) {
            logger.info("Find Component: {}", component);
        }
        Assertions.assertFalse(androidPackageComponents.isEmpty(), "androidPackageComponents is empty!");

        PkgUtil.handleAndroidPackageManifestElementsByTags(apkFile, element -> {
            NodeList intentFilter = element.getElementsByTagName("intent-filter");
            if (intentFilter.getLength() > 0) {
                logger.info("Find intent-filter, containing action: {}", element.getElementsByTagName("action").item(0).getAttributes().getNamedItem("android:name").getNodeValue());
            }
        }, "activity", "receiver", "service", "provider");
    }

    @Test
    public void parseTestAppContext() throws IOException {
        File apkFile = new File("src/test/resources/record_release.apk");
        TestAppContext testAppContext = PkgUtil.getTestAppContext(apkFile);
        logger.info("TestAppContext: {}", testAppContext);

        testAppContext.getAppComponents().forEach(appComponent -> {
            if (appComponent.getName().endsWith("ScreenRecorderService")) {
                Assertions.assertEquals(appComponent.getType(), "service", "ScreenRecorderService type error!");
            }
        });
    }

    @Test
    public void analysisIpaFile() {
        File recordFile = new File("src/test/resources/uitestsample.ipa");
        JSONObject res = PkgUtil.analysisFile(recordFile, EntityType.APP_FILE_SET);

        logger.info(res.toString(SerializerFeature.PrettyFormat));
        Assertions.assertEquals("com.microsoft.es.uitestsample", res.getString(StorageFileInfo.ParserKey.PKG_NAME), "Analysis ipa error!");
    }
}
