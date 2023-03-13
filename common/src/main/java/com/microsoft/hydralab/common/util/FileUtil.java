// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.http.HttpUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtil {
    private final static SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
    private static final Pattern paramKeyMatch = Pattern.compile("\\$\\{(\\w+)}");

    public static String UTF_8 = "UTF-8";

    public static String WORKSPACE_PATH = System.getProperty("user.dir");

    public static String getPathForToday() {
        Date date = new Date();
        return format.format(date);
    }

    public static String getLegalFileName(String originalFilename) {
        if (originalFilename == null) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Illegal file name!");
        }
        originalFilename = originalFilename.replaceAll(" ", "");
        String extension = FilenameUtils.getExtension(originalFilename);
        extension = extension.replaceAll("\\.", "").replaceAll("/", "");
        String fileName = FilenameUtils.getBaseName(originalFilename);
        fileName = fileName.replaceAll("\\.", "").replaceAll("/", "");
        if (StringUtils.isEmpty(extension) || StringUtils.isEmpty(fileName)) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Illegal file name!");
        }
        return fileName + "." + extension;
    }

    public static void deleteFileRecursively(File file) {
        File[] list = file.listFiles();
        if (list != null) {
            for (File temp : list) {
                deleteFileRecursively(temp);
            }
        }

        if (!file.delete()) {
            System.err.printf("Unable to delete file or directory : %s%n", file);
        }
    }

    public static void writeStringToFile(String data, File file) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            bufferedWriter.write(data);
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getDesktopPath() {
        return System.getProperty("user.home") + File.separator + "Desktop";
    }

    public static String getFileTextFromResource(String relativePath) {
        InputStream resourceAsStream = FileUtils.class.getClassLoader().getResourceAsStream(relativePath);
        try {
            return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getFileTextAsCommandFromResource(String relativePath) {
        return getFileTextFromResource(relativePath).trim().replaceAll("[\n\r]", "").replaceAll("[\\s]{2,}", " ");
    }

    public static String getFileTextAsCommandParamsTemplateFromResource(String relativePath, Map<String, String> params) {
        String templateStr = getFileTextAsCommandFromResource(relativePath);
        for (String key : params.keySet()) {
            if (!templateStr.contains(key)) {
                continue;
            }
            templateStr = templateStr.replace(String.format("${%s}", key), params.get(key).trim());
        }
        return templateStr.replaceAll(",? ?\\$\\{\\w+},?", "").replaceAll("[\\s]{2,}", " ");
    }

    public static String getStringFromFilePath(String path) {
        try {
            return IOUtils.toString(Files.newInputStream(new File(path).toPath()), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static List<String> getAllParamKeysInFile(String relativePath) {
        String text = getFileTextAsCommandFromResource(relativePath);
        Matcher matcher = paramKeyMatch.matcher(text);
        List<String> list = null;

        while (matcher.find()) {
            if (list == null) {
                list = new ArrayList<>();
            }
            String found = matcher.group(1);
            if (!list.contains(found)) {
                list.add(found);
            }
        }
        return list;
    }

    public static String getUrlFromEndpointInDir(String nginxStaticResEndpoint, String appDataPath, String fileAbsPath) {
        return nginxStaticResEndpoint + fileAbsPath.replace(File.separator, "/").replace(appDataPath, "");
    }

    public static String getOSCompatibleFromLinuxPath(String path) {
        return path.replace("/", File.separator);
    }

    public static String getSizeString(long size) {
        boolean isLessZero = false;
        if (size < 0) {
            size = -size;
            isLessZero = true;
        }
        int GB = 1024 * 1024 * 1024;
        int MB = 1024 * 1024;
        int KB = 1024;
        DecimalFormat df = new DecimalFormat("0.00");
        String resultSize = "";
        if (size / GB >= 1) {
            resultSize = df.format(size / (float) GB) + "GB";
        } else if (size / MB >= 1) {
            resultSize = df.format(size / (float) MB) + "MB";
        } else if (size / KB >= 1) {
            resultSize = df.format(size / (float) KB) + "KB";
        } else {
            resultSize = size + "B";
        }
        return isLessZero ? "-" + resultSize : resultSize;
    }

    public static String getSizeStringWithTagIfLarge(long size, long threshold) {
        if (size > threshold) {
            return String.format("<span style='color:red'>%s</span>", getSizeString(size));
        }
        return getSizeString(size);
    }

    public static long downloadFile(String fileUrl, File fileLocalPath, StreamProgress streamProgress) {
        return HttpUtil.downloadFile(fileUrl, fileLocalPath, streamProgress);
    }

    public static boolean isLegalFolderPath(String folderPath) {
        if (folderPath.endsWith("/")) {
            folderPath = folderPath.substring(0, folderPath.length() - 1);
        }
        String pattern = "^((?! )(?![^\\\\/]*\\s+[\\\\/])[\\w -]+[\\\\/])*(?! )(?![^.]*\\s+\\.)[\\w -]+$";
        return Pattern.matches(pattern, folderPath);
    }

    public static File unzipFile(String zipFilePath, String outFileDir) {
        return ZipUtil.unzip(zipFilePath, outFileDir);
    }

    public static File zipFile(String filePath, String zipFileDir) {
        return ZipUtil.zip(filePath, zipFileDir);
    }

    public static void deleteFile(File file) {
        cn.hutool.core.io.FileUtil.del(file);
    }

    public static void writeToFile(String report, String filePath) {
        File file = new File(filePath);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(report);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileWriter != null) {
                    fileWriter.flush();
                    fileWriter.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void downloadFileUsingStream(String urlStr, String file) throws IOException {
        URL url = new URL(urlStr);
        BufferedInputStream bis = new BufferedInputStream(url.openStream());
        FileOutputStream fis = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int count = 0;
        while ((count = bis.read(buffer, 0, 1024)) != -1) {
            fis.write(buffer, 0, count);
        }
        fis.close();
        bis.close();
    }
}
