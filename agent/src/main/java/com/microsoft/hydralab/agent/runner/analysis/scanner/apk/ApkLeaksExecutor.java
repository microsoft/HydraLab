// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.analysis.scanner.apk;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.common.scanner.ApkReport;
import com.microsoft.hydralab.common.entity.common.scanner.LeakInfo;
import com.microsoft.hydralab.common.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhoule
 * @date 11/16/2023
 */

public class ApkLeaksExecutor {
    static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final String CONFIG_FILE_PATH = "apk_leaks/apk_leaks_config.json";
    private static final String CONFIG_FILE_NAME = "apk_leaks_config.json";
    private final File configTemplate;
    private final File workingDir;

    public ApkLeaksExecutor(File folder) {
        workingDir = folder;
        if (!workingDir.exists()) {
            if (!workingDir.mkdirs()) {
                throw new RuntimeException("mkdir fail!");
            }
        }

        configTemplate = new File(folder, CONFIG_FILE_NAME);
        if (configTemplate.exists()) {
            configTemplate.delete();
        }
        try (InputStream resourceAsStream = FileUtils.class.getClassLoader().getResourceAsStream(CONFIG_FILE_PATH); OutputStream out = new FileOutputStream(configTemplate)) {
            IOUtils.copy(Objects.requireNonNull(resourceAsStream), out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ApkReport analyzeLeaks(ApkReport report, String apkPath, Map<String, String> sensitiveWords, Logger logger) {

        int code = -1;
        File apk = new File(apkPath);
        if (!apk.exists()) {
            throw new RuntimeException("apk not exist");
        }
        String name = apk.getName();
        String itemName = name.replace(".apk", "");
        String reportPrefix = itemName + "-leak-report";

        File reportFile = new File(workingDir, reportPrefix + ".json");
        if (reportFile.exists()) {
            reportFile.delete();
        }
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {

            String error = "";
            FileInputStream input = new FileInputStream(configTemplate);
            String content = IOUtils.toString(input, CHARSET);
            JSONObject jsonObject = JSONObject.parseObject(content);
            for (Map.Entry<String, String> entry : sensitiveWords.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
            input.close();

            String newConfigFileName = itemName + "-leak-config.json";
            File newConfigFile = new File(workingDir, newConfigFileName);
            if (newConfigFile.exists()) {
                newConfigFile.delete();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(newConfigFile);
            IOUtils.write(jsonObject.toJSONString(), fileOutputStream, CHARSET);
            fileOutputStream.close();

            String command = String.format("apkleaks -f %s -p %s -o %s --json", apk.getAbsoluteFile(), newConfigFile.getAbsolutePath(), reportFile.getAbsolutePath());
            process = runtime.exec(command, null, workingDir);

            try (InputStream inputStream = process.getInputStream();
                 InputStream errorStream = process.getErrorStream();
                 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    logger.info(line);
                }
                code = process.waitFor();
                error = IOUtils.toString(errorStream, CHARSET);
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }

            if (code == 0 && reportFile.exists()) {
                report.addReportFile(reportFile.getName());
                return getLeaksFromJsonReport(report, reportFile);
            }
            logger.error("error in apk leaks: {}", error);
        } catch (InterruptedException e) {
            logger.error("Interrupted in APK leaks scan", e);
        } catch (IOException e) {
            logger.error("error in APK leaks scan", e);
        }
        return report;
    }

    private ApkReport getLeaksFromJsonReport(ApkReport report, File file) {
        String json = FileUtil.getStringFromFilePath(file.getAbsolutePath());
        JSONObject objects = JSONObject.parseObject(json);
        JSONArray leakInfo = objects.getJSONArray("results");
        for (int i = 0; i < leakInfo.size(); i++) {
            JSONObject leak = leakInfo.getJSONObject(i);
            report.addLeakInfo(new LeakInfo(leak.getString("name"), leak.getJSONArray("matches").toJavaList(String.class)));
        }
        return report;
    }
}
