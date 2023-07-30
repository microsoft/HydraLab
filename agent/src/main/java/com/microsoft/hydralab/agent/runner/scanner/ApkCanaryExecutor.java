package com.microsoft.hydralab.agent.runner.scanner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.common.scanner.ApkManifest;
import com.microsoft.hydralab.common.entity.common.scanner.ApkReport;
import com.microsoft.hydralab.common.entity.common.scanner.ApkSizeReport;
import com.microsoft.hydralab.common.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ApkCanaryExecutor {
    /**
     * https://github.com/Tencent/matrix/blob/master/matrix/matrix-android/matrix-apk-canary/src/main/java/com/tencent/matrix/apk/model/task/TaskFactory.java
     */
    public static final int TASK_TYPE_UNZIP = 1;
    public static final int TASK_TYPE_MANIFEST = 2;
    public static final int TASK_TYPE_SHOW_FILE_SIZE = 3;
    public static final int TASK_TYPE_COUNT_METHOD = 4;
    public static final int TASK_TYPE_CHECK_RESGUARD = 5;
    public static final int TASK_TYPE_FIND_NON_ALPHA_PNG = 6;
    public static final int TASK_TYPE_CHECK_MULTILIB = 7;
    public static final int TASK_TYPE_UNCOMPRESSED_FILE = 8;
    public static final int TASK_TYPE_COUNT_R_CLASS = 9;
    public static final int TASK_TYPE_DUPLICATE_FILE = 10;
    public static final int TASK_TYPE_CHECK_MULTISTL = 11;
    public static final int TASK_TYPE_UNUSED_RESOURCES = 12;
    public static final int TASK_TYPE_UNUSED_ASSETS = 13;
    public static final int TASK_TYPE_UNSTRIPPED_SO = 14;
    public static final int TASK_TYPE_COUNT_CLASS = 15;
    public static final int DUP_FILE_MAX_COUNT = 5;
    public static final int ASSETS_LIST_MAX_COUNT = 20;
    private static final Logger LOGGER = LoggerFactory.getLogger(ApkCanaryExecutor.class);
    static final Charset CHARSET = StandardCharsets.UTF_8;

    public ApkReport analyzeApk(File canaryJar, File configTemplate, String apkPath, String workingDirPath) {
        File apk = new File(apkPath);
        if (!apk.exists()) {
            throw new RuntimeException("apk not exist");
        }
        File workingDirFile = new File(workingDirPath);
        if (!workingDirFile.exists()) {
            if (!workingDirFile.mkdirs()) {
                throw new RuntimeException("mkdir fail!");
            }
        }

        int code = -1;

        String name = apk.getName();
        String itemName = getAPKAnalysisItemName(name);
        String buildDirName = name.substring(0, name.indexOf('.')) + "_unzip";

        String reportPrefix = getReportPrefix(itemName);
        File reportFile = new File(workingDirFile, reportPrefix + ".json");
        if (reportFile.exists()) {
            reportFile.delete();
        }
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        try {
            String error = "";
            FileInputStream input = new FileInputStream(configTemplate);
            String content = IOUtils.toString(input, CHARSET);
            input.close();

            String newConfigFileName = itemName + "-config.json";
            File newConfigFile = new File(workingDirFile, newConfigFileName);
            if (newConfigFile.exists()) {
                newConfigFile.delete();
            }

            FileOutputStream fileOutputStream = new FileOutputStream(newConfigFile);
            IOUtils.write(
                    content.replace("@NAME_HOLDER", apk.getAbsolutePath().replace("\\", "\\\\"))
                            .replace("@REPORT_NAME_HOLDER", new File(workingDirFile, reportPrefix).getAbsolutePath().replace("\\", "\\\\")),
                    fileOutputStream, CHARSET);

            fileOutputStream.close();

            String command = String.format("java -jar %s --config %s", canaryJar.getAbsoluteFile(), newConfigFile.getAbsolutePath());
            LOGGER.info(command);
            process = runtime.exec(command, null, workingDirFile);

            try (InputStream inputStream = process.getInputStream();
                 InputStream errorStream = process.getErrorStream();
                 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    LOGGER.info(line);
                }
                code = process.waitFor();
                error = IOUtils.toString(errorStream, CHARSET);
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
            FileUtils.deleteDirectory(new File(workingDirFile, buildDirName));

            if (code != 0) {
                LOGGER.info(error);
                throw new RuntimeException("failed for " + name);
            }

            return getApkReportFromJsonReport(reportFile);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("error in analyzeApk", e);
        }
        return null;
    }

    public static String getAPKAnalysisItemName(String apkName) {
        return apkName.replace("-signed-aligned", "").replace(".apk", "");
    }

    public static String getReportPrefix(String itemName) {
        return itemName + "-report";
    }

    public static ApkReport getApkReportFromJsonReport(File file) {
        String json = FileUtil.getStringFromFilePath(file.getAbsolutePath());
        JSONArray objects = JSON.parseArray(json);

        ApkReport apkReport = new ApkReport("apkReport");

        for (int i = 0; i < objects.size(); i++) {
            JSONObject jsonObject = objects.getJSONObject(i);
            int taskType = jsonObject.getIntValue("taskType");
            if (taskType == TASK_TYPE_UNZIP) {
                parseSizeInfo(apkReport.getApkSizeReport(), jsonObject);
            } else if (taskType == TASK_TYPE_MANIFEST) {
                parseManifest(apkReport.getApkManifest(), jsonObject);
            } else if (taskType == TASK_TYPE_DUPLICATE_FILE) {
                parseDupFile(apkReport.getApkSizeReport(), jsonObject);
            } else if (taskType == TASK_TYPE_UNUSED_ASSETS) {
                parseUnusedAssets(apkReport.getApkSizeReport(), jsonObject);
            } else if (taskType == TASK_TYPE_SHOW_FILE_SIZE) {
                parseBigFiles(apkReport.getApkSizeReport(), jsonObject);
            }
        }
        return apkReport;
    }

    private static void parseBigFiles(ApkSizeReport apkSizeReport, JSONObject jsonObject) {
        JSONArray bigFiles = jsonObject.getJSONArray("files");
        if (bigFiles != null) {
            for (int n = 0; n < bigFiles.size(); n++) {
                JSONObject fileObject = bigFiles.getJSONObject(n);
                ApkSizeReport.FileItem fileItem = new ApkSizeReport.FileItem();
                fileItem.fileName = fileObject.getString("entry-name");
                if (fileItem.fileName != null && fileItem.fileName.equals("resources.arsc")) {
                    continue;
                }
                fileItem.size = fileObject.getLongValue("entry-size");
                apkSizeReport.bigSizeFileList.add(fileItem);
            }
        }
    }

    private static void parseUnusedAssets(ApkSizeReport apkSizeReport, JSONObject jsonObject) {
        String unusedStr = jsonObject.getString("unused-assets");
        apkSizeReport.unusedAssetsList.addAll(parseUnusedAssetsList(JSONArray.parseArray(unusedStr, String.class)));
        if (apkSizeReport.unusedAssetsList.size() > ASSETS_LIST_MAX_COUNT) {
            apkSizeReport.unusedAssetsList = apkSizeReport.unusedAssetsList.subList(0, ASSETS_LIST_MAX_COUNT);
        }
    }

    private static void parseDupFile(ApkSizeReport apkSizeReport, JSONObject jsonObject) {
        JSONArray dupFiles = jsonObject.getJSONArray("files");
        if (dupFiles != null) {
            for (int n = 0; n < Math.min(dupFiles.size(), DUP_FILE_MAX_COUNT); n++) {
                ApkSizeReport.DuplicatedFile duplicatedFile = new ApkSizeReport.DuplicatedFile();
                JSONObject dupFileObject = dupFiles.getJSONObject(n);
                duplicatedFile.md5 = dupFileObject.getString("md5");
                duplicatedFile.size = dupFileObject.getLongValue("size");
                String fileString = dupFileObject.getString("files");
                duplicatedFile.fileList = JSONArray.parseArray(fileString, String.class);
                apkSizeReport.getDuplicatedFileList().add(duplicatedFile);
            }
        }
    }

    private static void parseManifest(ApkManifest apkManifest, JSONObject jsonObject) {
        JSONObject manifest = jsonObject.getJSONObject("manifest");
        apkManifest.setPackageName(manifest.getString("package"));
        apkManifest.setMinSDKVersion(Integer.parseInt(manifest.getString("android:minSdkVersion")));
        apkManifest.setTargetSDKVersion(Integer.parseInt(manifest.getString("android:targetSdkVersion")));
        apkManifest.setVersionCode(Integer.parseInt(manifest.getString("android:versionCode")));
        apkManifest.setVersionName(manifest.getString("android:versionName"));
    }

    private static void parseSizeInfo(ApkSizeReport apkSizeReport, JSONObject jsonObject) {
        long total = jsonObject.getLongValue("total-size");
        apkSizeReport.setTotalSize(total);
        apkSizeReport.setTotalSizeInMB(total * 1f / 1024 / 1024);

        long otherSize = total;
        JSONArray entries = jsonObject.getJSONArray("entries");

        for (int n = 0; n < entries.size(); n++) {
            JSONObject en = entries.getJSONObject(n);
            String suffix = en.getString("suffix");
            long entryTotalSize = en.getLongValue("total-size");
            switch (suffix) {
                case ".arsc":
                    apkSizeReport.setArscSize(entryTotalSize);
                    otherSize -= entryTotalSize;
                    break;
                case ".dex":
                    apkSizeReport.setDexSize(entryTotalSize);
                    otherSize -= entryTotalSize;
                    break;
                case ".png":
                    apkSizeReport.setPngSize(entryTotalSize);
                    otherSize -= entryTotalSize;
                    break;
                case ".xml":
                    apkSizeReport.setXmlSize(entryTotalSize);
                    otherSize -= entryTotalSize;
                    break;
                case ".so":
                    apkSizeReport.setSoSize(entryTotalSize);
                    otherSize -= entryTotalSize;
                    break;
                case ".webp":
                    apkSizeReport.setWebpSize(entryTotalSize);
                    otherSize -= entryTotalSize;
                    break;
                default:
                    break;
            }
        }
        apkSizeReport.setOtherSize(otherSize);
    }

    public static List<ApkSizeReport.FileItem> parseUnusedAssetsList(List<String> unusedFileNameList) {
        List<ApkSizeReport.FileItem> unusedAssetsList = new ArrayList<>();
        for (String fileName : unusedFileNameList) {
            ApkSizeReport.FileItem fileItem = new ApkSizeReport.FileItem();
            unusedAssetsList.add(fileItem);
            fileItem.fileName = "assets/" + fileName;
        }
        return unusedAssetsList;
    }
}
