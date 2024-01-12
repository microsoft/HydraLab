package com.microsoft.hydralab.agent.runner.scanner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.microsoft.hydralab.agent.runner.analysis.scanner.apk.ApkCanaryExecutor;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.entity.common.scanner.ApkReport;
import com.microsoft.hydralab.common.util.FileUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class ApkCanaryExecutorTest extends BaseTest {

    @Test
    public void testApkCanaryExecutor() {
        File apkFilePath = new File("../common/src/test/resources/record_release.apk");
        Assertions.assertTrue(apkFilePath.exists(), "apk file not exist: " + apkFilePath.getAbsolutePath());
        File workingDir = new File("../common/src/test/resources/apk_canary");
        ApkCanaryExecutor apkCanaryExecutor = new ApkCanaryExecutor(workingDir);
        ApkReport apkReport = new ApkReport("apkReport");
        apkReport = apkCanaryExecutor.analyzeApk(
                apkReport,
                apkFilePath.getAbsolutePath(),
                baseLogger
        );
        FileUtil.deleteFileRecursively(workingDir);
        System.out.println(JSON.toJSONString(apkReport, SerializerFeature.PrettyFormat));
    }

    @Test
    public void testApkReportParsing() {
        File apkJsonReportFile = new File("src/test/resources/MicrosoftLauncherAPKReport.json");
        Assertions.assertTrue(apkJsonReportFile.exists(), "apkJsonReportFile does not exist: " + apkJsonReportFile.getAbsolutePath());
        ApkReport apkReport = new ApkReport("apkReport");
        apkReport = ApkCanaryExecutor.getApkReportFromJsonReport(apkReport, apkJsonReportFile);
        System.out.println(JSON.toJSONString(apkReport, SerializerFeature.PrettyFormat));
    }

}
