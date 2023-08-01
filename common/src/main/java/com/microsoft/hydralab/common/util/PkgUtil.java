// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import cn.hutool.core.util.ZipUtil;
import com.alibaba.fastjson.JSONObject;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListParser;
import com.microsoft.hydralab.common.entity.common.AgentUpdateTask.TaskConst;
import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo.ParserKey;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.apache.commons.io.FileUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PkgUtil {
    public static JSONObject analysisFile(File file, EntityType entityType) {
        JSONObject res = new JSONObject();
        switch (entityType) {
            case APP_FILE_SET:
                if (file.getName().endsWith(FILE_SUFFIX.APK_FILE)) {
                    res = analysisApkFile(file);
                } else if (file.getName().endsWith(FILE_SUFFIX.IPA_FILE)) {
                    res = analysisIpaFile(file);
                } else if (file.getName().endsWith(FILE_SUFFIX.ZIP_FILE)) {
                    res = analysisZipFile(file);
                }

                break;
            case AGENT_PACKAGE:
                res = getAgentVersionFromJarFile(file);
                break;
            default:
                break;
        }
        return res;
    }

    private static JSONObject getAgentVersionFromJarFile(File file) {
        JSONObject res = new JSONObject();
        InputStream propertyStream = null;
        File zipFile = null;

        try {
            zipFile = convertToZipFile(file, FILE_SUFFIX.JAR_FILE);
            Assert.notNull(zipFile, "Convert .jar file to .zip file failed.");
            propertyStream = ZipUtil.get(new ZipFile(zipFile), TaskConst.PROPERTY_PATH);
            Properties prop = new Properties();
            prop.load(propertyStream);

            res.put(TaskConst.PARAM_VERSION_NAME, prop.getProperty(TaskConst.PROPERTY_VERSION_NAME));
            res.put(TaskConst.PARAM_VERSION_CODE, prop.getProperty(TaskConst.PROPERTY_VERSION_CODE));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (propertyStream != null) {
                try {
                    propertyStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (zipFile.exists()) {
                zipFile.delete();
            }
        }
        return res;
    }

    private static JSONObject analysisApkFile(File file) {
        JSONObject res = new JSONObject();
        try (ApkFile apkFile = new ApkFile(file)) {
            ApkMeta apkMeta = apkFile.getApkMeta();
            res.put(ParserKey.APP_NAME, apkMeta.getName());
            res.put(ParserKey.PKG_NAME, apkMeta.getPackageName());
            res.put(ParserKey.VERSION, apkMeta.getVersionName());
            res.put(ParserKey.MIN_SDK_VERSION, apkMeta.getMinSdkVersion());
            res.put(ParserKey.TARGET_SDK_VERSION, apkMeta.getTargetSdkVersion());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BufferUnderflowException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    private static JSONObject analysisIpaFile(File ipa) {
        JSONObject res = new JSONObject();
        try {
            String name, pkgName, version;
            File zipFile = convertToZipFile(ipa, FILE_SUFFIX.IPA_FILE);
            Assert.notNull(zipFile, "Convert .ipa file to .zip file failed.");
            File file = getPlistFromZip(zipFile, zipFile.getParent());
            //Need third-party jar package dd-plist
            Assert.notNull(file, "Analysis .ipa file failed.");
            analysisPlist(file, res);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    private static JSONObject analysisZipFile(File zip) {
        JSONObject res = new JSONObject();
        try {
            String unzippedFolderPath = zip.getParentFile().getAbsolutePath()
                    + "/" + zip.getName().substring(0, zip.getName().lastIndexOf('.'));
            FileUtil.unzipFile(zip.getAbsolutePath(), unzippedFolderPath);
            File unzippedFolder = new File(unzippedFolderPath);
            // for XCTest package
            File plistFile = getPlistFromFolder(unzippedFolder);
            // for maestro case
            List<File> yamlFiles = getYamlFromFolder(unzippedFolder);
            if (plistFile != null) {
                analysisPlist(plistFile, res);
            } else if (yamlFiles.size() == 0) {
                throw new HydraLabRuntimeException("Analysis .zip file failed. It's not a valid XCTEST package or maestro case.");
            }
            FileUtil.deleteFile(unzippedFolder);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;

    }

    private static void analysisPlist(File plistFile, JSONObject res) throws Exception {
        String name, pkgName, version;
        NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(plistFile);
        //Application package name
        NSString parameters = (NSString) rootDict.objectForKey("CFBundleIdentifier");
        pkgName = parameters.toString();
        //Application version
        parameters = (NSString) rootDict.objectForKey("CFBundleVersion");
        version = parameters.toString();
        //Application display name
        parameters = (NSString) rootDict.objectForKey("CFBundleDisplayName");
        name = parameters.toString();

        //If necessary, the decompressed files should be deleted
        plistFile.delete();
        plistFile.getParentFile().delete();

        res.put(ParserKey.APP_NAME, name);
        res.put(ParserKey.PKG_NAME, pkgName);
        res.put(ParserKey.VERSION, version);
    }

    private static File getPlistFromZip(File file, String unzipDirectory) throws Exception {
        //Define input and output stream objects
        InputStream input = null;
        OutputStream output = null;
        File result = null;
        File unzipFile = null;
        ZipFile zipFile = null;
        try {
            //Create zip file object
            zipFile = new ZipFile(file);
            //Create this zip file decompression directory
            String name = file.getName().substring(0, file.getName().lastIndexOf("."));
            unzipFile = new File(unzipDirectory + "/" + name);
            if (unzipFile.exists()) {
                unzipFile.delete();
            }
            unzipFile.mkdir();
            //Get zip file entry enumeration object
            Enumeration<? extends ZipEntry> zipEnum = zipFile.entries();
            //define object
            ZipEntry entry = null;
            String entryName = null;
            String[] names = null;
            int length;
            //loop reading entries
            while (zipEnum.hasMoreElements()) {
                //get the current entry
                entry = zipEnum.nextElement();
                entryName = new String(entry.getName());
                //separate entry names with/
                names = entryName.split("\\/");
                length = names.length;
                for (int v = 0; v < length; v++) {
                    if (entryName.endsWith(".app/Info.plist")) {//is Info.plist file, then output to file
                        input = zipFile.getInputStream(entry);
                        result = new File(unzipFile.getAbsolutePath() + "/Info.plist");
                        output = Files.newOutputStream(result.toPath());
                        byte[] buffer = new byte[1024 * 8];
                        int readLen = 0;
                        while ((readLen = input.read(buffer, 0, 1024 * 8)) != -1) {
                            output.write(buffer, 0, readLen);
                        }
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.flush();
                output.close();
            }
            //The stream must be closed, otherwise the file cannot be deleted
            if (zipFile != null) {
                zipFile.close();
            }
        }

        //Delete extra files if necessary
        if (file.exists()) {
            file.delete();
        }
        return result;
    }

    private static File getPlistFromFolder(File rootFolder) {
        Collection<File> files = FileUtils.listFiles(rootFolder, null, true);
        for (File file : files) {
            if (file.getAbsolutePath().endsWith(".app/Info.plist")
                    && !file.getAbsolutePath().contains("-Runner")
                    && !file.getAbsolutePath().contains("Watch")) {
                return file;
            }
        }
        return null;
    }

    private static List<File> getYamlFromFolder(File rootFolder) {
        Collection<File> files = FileUtils.listFiles(rootFolder, null, true);
        List<File> yamlFiles = new ArrayList<>();
        for (File file : files) {
            if (file.getAbsolutePath().endsWith(".yaml")) {
                yamlFiles.add(file);
            }
        }
        return yamlFiles;
    }

    private static File convertToZipFile(File file, String suffix) {
        try {
            int bytes = 0;
            String filename = file.getAbsolutePath().replaceAll(suffix, FILE_SUFFIX.ZIP_FILE);
            File zipFile = new File(filename);
            if (file.exists()) {
                //Create a Zip file
                InputStream inStream = new FileInputStream(file);
                FileOutputStream fs = new FileOutputStream(zipFile);
                byte[] buffer = new byte[1444];
                while ((bytes = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, bytes);
                }
                inStream.close();
                fs.close();
                //parse the Zip file
                return zipFile;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public interface FILE_SUFFIX {
        String APK_FILE = ".apk";
        String JAR_FILE = ".jar";
        String APPX_FILE = ".appxbundle";
        String ZIP_FILE = ".zip";
        String IPA_FILE = ".ipa";
        String JSON_FILE = ".json";
        String APP_FILE = ".app";
    }
}
