// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import cn.hutool.core.util.ZipUtil;
import com.alibaba.fastjson.JSONObject;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListParser;
import com.microsoft.hydralab.common.entity.common.AgentUpdateTask;
import com.microsoft.hydralab.common.entity.common.BlobFileInfo.ParserKey;
import com.microsoft.hydralab.common.entity.common.EntityFileRelation;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.springframework.util.Assert;

import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PkgUtil {
    public static JSONObject analysisFile(File file, EntityFileRelation.EntityType entityType) {
        JSONObject res = new JSONObject();
        switch (entityType) {
            case APP_FILE_SET:
                if (file.getName().endsWith(FILE_SUFFIX.APK_FILE)) {
                    res = analysisApkFile(file);
                } else if (file.getName().endsWith(FILE_SUFFIX.IPA_FILE)) {
                    res = analysisIpaFile(file);
                }
                break;
            case AGENT_PACKAGE:
                String version = getAgentVersionFromJarFile(file);
                Assert.notNull(version, "Agent Package File Error! Can't get Version Info");
                res.put(AgentUpdateTask.TaskConst.PARAM_VERSION, version);
                break;
            default:
                break;
        }


        return res;
    }

    private static String getAgentVersionFromJarFile(File file) {
        String version = null;
        InputStream propertyStream = null;
        File zipFile = null;
        try {
            zipFile = convertToZipFile(file, FILE_SUFFIX.JAR_FILE);
            Assert.notNull(zipFile, "Convert .jar file to .zip file failed.");
            propertyStream = ZipUtil.get(new ZipFile(zipFile), AgentUpdateTask.TaskConst.PROPERTY_PATH);
            Properties prop = new Properties();
            prop.load(propertyStream);

            version = prop.getProperty(AgentUpdateTask.TaskConst.PROPERTY_VERSION);
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
        return version;
    }

    public static JSONObject analysisApkFile(File file) {
        JSONObject res = new JSONObject();
        try (ApkFile apkFile = new ApkFile(file)) {
            ApkMeta apkMeta = apkFile.getApkMeta();
            res.put(ParserKey.AppName, apkMeta.getName());
            res.put(ParserKey.PkgName, apkMeta.getPackageName());
            res.put(ParserKey.Version, apkMeta.getVersionName());
            res.put(ParserKey.MinSdkVersion, apkMeta.getMinSdkVersion());
            res.put(ParserKey.TargetSdkVersion, apkMeta.getTargetSdkVersion());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BufferUnderflowException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static JSONObject analysisIpaFile(File ipa) {
        JSONObject res = new JSONObject();
        try {
            String name, pkgName, version;
            File zipFile = convertToZipFile(ipa, FILE_SUFFIX.IPA_FILE);
            Assert.notNull(zipFile, "Convert .ipa file to .zip file failed.");
            File file = getIpaPlistFile(zipFile, zipFile.getParent());
            //Need third-party jar package dd-plist
            Assert.notNull(file, "Analysis .ipa file failed.");
            NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(file);
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
            file.delete();
            file.getParentFile().delete();

            res.put(ParserKey.AppName, name);
            res.put(ParserKey.PkgName, pkgName);
            res.put(ParserKey.Version, version);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    private static File getIpaPlistFile(File file, String unzipDirectory) throws Exception {
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
    }
}
