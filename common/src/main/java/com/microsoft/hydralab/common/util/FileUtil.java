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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
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

public final class FileUtil {
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy" + File.separator + "MM" + File.separator + "dd");
    private static final Pattern PARAM_KEY_MATCH = Pattern.compile("\\$\\{(\\w+)}");

    @SuppressWarnings({"StaticVariableName", "VisibilityModifier"})
    public static String UTF_8 = "UTF-8";

    private static final String WORKSPACE_PATH = System.getProperty("user.dir");

    private static final int GB = 1024 * 1024 * 1024;
    private static final int MB = 1024 * 1024;
    private static final int KB = 1024;

    private FileUtil() {

    }

    public static String getPathForToday() {
        Date date = new Date();
        return FORMAT.format(date);
    }

    public static String getLegalFileName(String originalFilename) {
        if (originalFilename == null) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Illegal file name!");
        }
        String tmpName = originalFilename.replaceAll(" ", "");
        String extension = FilenameUtils.getExtension(tmpName);
        extension = extension.replaceAll("\\.", "").replaceAll("/", "");
        String fileName = FilenameUtils.getBaseName(tmpName);
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
        Matcher matcher = PARAM_KEY_MATCH.matcher(text);
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
        long tmpSize = size;
        if (tmpSize < 0) {
            tmpSize = -tmpSize;
            isLessZero = true;
        }
        DecimalFormat df = new DecimalFormat("0.00");
        String resultSize = "";
        if (tmpSize / GB >= 1) {
            resultSize = df.format(tmpSize / (float) GB) + "GB";
        } else if (tmpSize / MB >= 1) {
            resultSize = df.format(tmpSize / (float) MB) + "MB";
        } else if (tmpSize / KB >= 1) {
            resultSize = df.format(tmpSize / (float) KB) + "KB";
        } else {
            resultSize = tmpSize + "B";
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
        String tmpFolderPath = folderPath;
        if (tmpFolderPath.endsWith("/")) {
            tmpFolderPath = tmpFolderPath.substring(0, tmpFolderPath.length() - 1);
        }
        String pattern = "^((?! )(?![^\\\\/]*\\s+[\\\\/])[\\w -]+[\\\\/])*(?! )(?![^.]*\\s+\\.)[\\w -]+$";
        return Pattern.matches(pattern, tmpFolderPath);
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
