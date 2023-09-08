package com.microsoft.hydralab.agent.runner.scanner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.microsoft.hydralab.common.entity.common.scanner.ApkReport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class ApkCanaryExecutorTest {

    @Test
    public void testApkCanaryExecutor() {
        File apkFilePath = new File("../common/src/test/resources/record_release.apk");
        Assertions.assertTrue(apkFilePath.exists(), "apk file not exist: " + apkFilePath.getAbsolutePath());
        ApkCanaryExecutor apkCanaryExecutor = new ApkCanaryExecutor();
        ApkReport apkReport = apkCanaryExecutor.analyzeApk(
                new File("src/main/resources/apk_canary/matrix-apk-canary-2.1.0.jar"),
                new File("src/main/resources/apk_canary/apk_canary_config_template.json"),
                apkFilePath.getAbsolutePath(),
                new File("").getAbsolutePath()
        );
        System.out.println(JSON.toJSONString(apkReport, SerializerFeature.PrettyFormat));
    }

}
