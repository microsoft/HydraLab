package com.microsoft.hydralab.agent.runner.scanner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.microsoft.hydralab.common.entity.common.scanner.ApkReport;
import com.microsoft.hydralab.common.util.FileUtil;
import org.junit.jupiter.api.Test;

import java.io.File;

public class ApkCanaryExecutorTest {

    @Test
    public void testApkCanaryExecutor() {
        File apkFilePath = new File("build/record_release.apk");
        FileUtil.downloadFile("https://raw.githubusercontent.com/microsoft/HydraLab/Release/1.12.0/common/src/main/resources/record_release.apk",
                apkFilePath, null);
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
