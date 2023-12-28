package com.microsoft.hydralab.agent.runner.scanner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.microsoft.hydralab.agent.runner.analysis.scanner.apk.ApkCanaryExecutor;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.entity.common.scanner.ApkReport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class ApkCanaryExecutorTest extends BaseTest {

    @Test
    public void testApkCanaryExecutor() {
        File apkFilePath = new File("C:\\Users\\zhoule\\Downloads\\Link_to_Windows_1.23102.199.0_Apkpure.apk");
        Assertions.assertTrue(apkFilePath.exists(), "apk file not exist: " + apkFilePath.getAbsolutePath());
        ApkCanaryExecutor apkCanaryExecutor = new ApkCanaryExecutor(new File(""));
        ApkReport apkReport = new ApkReport("apkReport");
        apkReport = apkCanaryExecutor.analyzeApk(
                apkReport,
                apkFilePath.getAbsolutePath(),
                baseLogger

        );
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
