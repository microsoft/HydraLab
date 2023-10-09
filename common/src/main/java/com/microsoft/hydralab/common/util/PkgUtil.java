// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import cn.hutool.core.util.ZipUtil;
import com.alibaba.fastjson.JSONObject;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListParser;
import com.microsoft.hydralab.common.entity.common.AgentUpdateTask.TaskConst;
import com.microsoft.hydralab.common.entity.common.AppComponent;
import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo.ParserKey;
import com.microsoft.hydralab.common.entity.common.TestAppContext;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PkgUtil {
    static Logger classLogger = LoggerFactory.getLogger(PkgUtil.class);

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

    public static String getAndroidPackageManifest(File apkFile) throws IOException {
        try (ApkFile apkFileObj = new ApkFile(apkFile)) {
            return apkFileObj.getManifestXml();
        }
    }

    public static List<String> getAndroidPackageComponents(File apkFile) {
        return getAndroidPackageManifestTagAttrVal(apkFile, "android:name", "activity", "service", "receiver", "provider");
    }

    public static List<String> getAndroidPackageActivities(File apkFile) {
        return getAndroidPackageManifestTagAttrVal(apkFile, "android:name", "activity");
    }

    public static List<String> getAndroidPackageManifestTagAttrVal(File apkFile, String attrName, String... tags) {
        try {
            // get the manifest xml from the apk file and get all the activity nodes
            String manifestXml = getAndroidPackageManifest(apkFile);
            return getAndroidPackageManifestTagAttrVal(manifestXml, attrName, tags);
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
        }
        return null;
    }

    public static List<String> getAndroidPackageManifestTagAttrVal(String manifestXml, String attrName, String... tags) {
        List<String> values = null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(manifestXml.getBytes(StandardCharsets.UTF_8))) {
            // get the manifest xml from the apk file and get all the activity nodes
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 创建一个 DocumentBuilder 对象
            DocumentBuilder builder = factory.newDocumentBuilder();
            // 使用DocumentBuilder对象解析XML
            Document document = builder.parse(bais);
            // 标准化XML，可选步骤
            document.getDocumentElement().normalize();
            // 获取所有名为"activity"的节点
            List<Element> elements = getElementListByTags(document, tags);
            values = new ArrayList<>();
            // 遍历每一个节点
            for (Element element : elements) {
                String activityName = element.getAttribute(attrName);
                values.add(activityName);
            }
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
        }
        return values;
    }

    public static void handleAndroidPackageManifestElementsByTags(File apkFile, Consumer<Element> consumer, String... tags) {
        try {
            // get the manifest xml from the apk file and get all the activity nodes
            String manifestXml = getAndroidPackageManifest(apkFile);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(manifestXml.getBytes(StandardCharsets.UTF_8))) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                // 创建一个 DocumentBuilder 对象
                DocumentBuilder builder = factory.newDocumentBuilder();
                // 使用DocumentBuilder对象解析XML
                Document document = builder.parse(bais);
                // 标准化XML，可选步骤
                document.getDocumentElement().normalize();
                // 获取所有名为"activity"的节点
                List<Element> elements = getElementListByTags(document, tags);
                // 遍历每一个节点
                for (Element element : elements) {
                    consumer.accept(element);
                }
            }
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
        }
    }

    public static void handleAndroidPackageManifestElementsByTags(String manifestXml, Consumer<Element> consumer, String... tags) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(manifestXml.getBytes(StandardCharsets.UTF_8))) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 创建一个 DocumentBuilder 对象
            DocumentBuilder builder = factory.newDocumentBuilder();
            // 使用DocumentBuilder对象解析XML
            Document document = builder.parse(bais);
            // 标准化XML，可选步骤
            document.getDocumentElement().normalize();
            // 获取所有名为"activity"的节点
            List<Element> elements = getElementListByTags(document, tags);
            // 遍历每一个节点
            for (Element element : elements) {
                consumer.accept(element);
            }
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
        }
    }

    private static List<Element> getElementListByTags(Document document, String... tags) {
        Assert.notNull(tags, "Element tags can not be null");
        ArrayList<Element> elements = new ArrayList<>();
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            NodeList nodeList = document.getElementsByTagName(tag);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    elements.add((Element) node);
                }
            }
        }
        return elements;
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
            classLogger.error(e.getMessage(), e);
        } finally {
            if (propertyStream != null) {
                try {
                    propertyStream.close();
                } catch (IOException e) {
                    classLogger.error(e.getMessage(), e);
                }
            }
            if (zipFile != null && zipFile.exists()) {
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
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
        }
        return res;
    }

    private static JSONObject analysisIpaFile(File ipa) {
        JSONObject res = new JSONObject();
        try {
            File zipFile = convertToZipFile(ipa, FILE_SUFFIX.IPA_FILE);
            Assert.notNull(zipFile, "Convert .ipa file to .zip file failed.");
            File file = getPlistFromZip(zipFile, zipFile.getParent());
            //Need third-party jar package dd-plist
            Assert.notNull(file, "Analysis .ipa file failed.");
            analysisPlist(file, res);
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
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
            // for Python case
            File pyMainFile = getPyFromFolder(unzippedFolder);
            if (plistFile != null) {
                analysisPlist(plistFile, res);
            } else if (pyMainFile != null) {
                res.put(ParserKey.APP_NAME, "Python Runner");
                res.put(ParserKey.PKG_NAME, "Python Runner");
            } else if (yamlFiles.isEmpty()) {
                classLogger.warn("Analysis .zip file failed. It's not a valid XCTEST package, Maestro case or Python package.");
            }
            FileUtil.deleteFile(unzippedFolder);
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
        }
        return res;

    }

    private static File getPyFromFolder(File rootFolder) {
        Collection<File> files = FileUtils.listFiles(rootFolder, null, true);
        for (File file : files) {
            if ("main.py".equals(file.getName())) {
                return file;
            }
        }
        return null;
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
        File unzipFile;
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
            ZipEntry entry;
            String entryName;
            String[] names;
            int length;
            //loop reading entries
            while (zipEnum.hasMoreElements()) {
                //get the current entry
                entry = zipEnum.nextElement();
                entryName = entry.getName();
                //separate entry names with/
                names = entryName.split("\\/");
                length = names.length;
                for (int v = 0; v < length; v++) {
                    if (entryName.endsWith(".app/Info.plist")) {//is Info.plist file, then output to file
                        input = zipFile.getInputStream(entry);
                        result = new File(unzipFile.getAbsolutePath() + "/Info.plist");
                        output = Files.newOutputStream(result.toPath());
                        byte[] buffer = new byte[1024 * 8];
                        int readLen;
                        while ((readLen = input.read(buffer, 0, 1024 * 8)) != -1) {
                            output.write(buffer, 0, readLen);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            classLogger.error(e.getMessage(), e);
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
            int bytes;
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
            classLogger.error(e.getMessage(), e);
        }
        return null;
    }

    public static TestAppContext getTestAppContext(File apkFile) throws IOException {
        try (ApkFile apkFileObj = new ApkFile(apkFile)) {
            TestAppContext testAppContext = new TestAppContext();
            ApkMeta apkMeta = apkFileObj.getApkMeta();
            testAppContext.setAppName(apkMeta.getName());
            testAppContext.setPackageName(apkMeta.getPackageName());
            testAppContext.setPackageSize(apkFile.length());
            testAppContext.setAppVersion(apkMeta.getVersionName());
            handleAndroidPackageManifestElementsByTags(apkFileObj.getManifestXml(), element -> {
                String tagName = element.getTagName();
                String attributeNameVal = element.getAttribute("android:name");
                if (Objects.equals(tagName, "activity")) {
                    testAppContext.getAppComponents().add(new AppComponent(attributeNameVal));
                } else if (Objects.equals(tagName, "provider")) {
                    testAppContext.getAppComponents().add(new AppComponent(attributeNameVal, AppComponent.Type.DATASOURCE));
                } else {
                    testAppContext.getAppComponents().add(new AppComponent(attributeNameVal, AppComponent.Type.SERVICE));
                }
            }, "activity", "service", "receiver", "provider");

            return testAppContext;
        }
    }

    public interface FILE_SUFFIX {
        String APK_FILE = ".apk";
        String JAR_FILE = ".jar";
        String APPX_FILE = ".appxbundle";
        String ZIP_FILE = ".zip";
        String IPA_FILE = ".ipa";
        String JSON_FILE = ".json";
//        String APP_FILE = ".app";
    }
}
