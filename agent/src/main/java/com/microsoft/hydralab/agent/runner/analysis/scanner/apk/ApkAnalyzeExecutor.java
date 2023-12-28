// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.analysis.scanner.apk;

import com.microsoft.hydralab.common.entity.common.scanner.ApkReport;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;

/**
 * @author zhoule
 * @date 12/28/2023
 */

public class ApkAnalyzeExecutor {
    static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final String EXECUTOR_TYPE = "apk_analyzer";
    private final File workingDir;

    public ApkAnalyzeExecutor(File outputFolder) {
        workingDir = outputFolder;
        if (!workingDir.exists()) {
            if (!workingDir.mkdirs()) {
                throw new RuntimeException("mkdir fail!");
            }
        }
    }

    public ApkReport analyzeApk(ApkReport report, String apkPath, Logger logger) {
        File apk = new File(apkPath);
        if (!apk.exists()) {
            throw new RuntimeException("apk not exist");
        }
        String name = apk.getName();
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        int code = -1;
        String error = "";
        String result = "";
        try {
            String command = String.format("apkanalyzer apk download-size %s", apk.getAbsolutePath());
            process = runtime.exec(command, null, workingDir);
            try (InputStream inputStream = process.getInputStream();
                 InputStream errorStream = process.getErrorStream();
                 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    logger.info(line);
                    result += line;
                }
                code = process.waitFor();
                error = IOUtils.toString(errorStream, CHARSET);
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
            if (code == 0) {
                try {
                    int downloadSize = Integer.parseInt(result);
                    report.getApkSizeReport().setDownloadSize(downloadSize);
                    Formatter size = new Formatter().format("%.2f", downloadSize / 1024 / 1024);
                    report.getApkSizeReport().setDownloadSizeInMB(Long.valueOf(size.toString()));
                } catch (Exception e) {
                    logger.info("failed to get download size");
                }
            }
            logger.error("error in apk analyzer: {}", error);

        } catch (InterruptedException e) {
            logger.error("Interrupted in APK analyser", e);
        } catch (IOException e) {
            logger.error("error in APK analyser", e);
        }
        return report;
    }

}
