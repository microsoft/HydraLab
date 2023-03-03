// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.util.DigestUtils;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

@Entity
@Data
@Table(name = "blob_file_info")
public class StorageFileInfo implements Serializable {
    @Id
    @Column(name = "file_id", nullable = false)
    private String fileId;
    private String fileType;
    private String fileName;

    private String blobUrl;
    private String blobContainer;
    // relative to blobContainer
    private String blobPath;

    private long fileLen;
    private String md5;
    private String loadDir;
    private String loadType;
    @Convert(converter = JsonConverter.class)
    private JSONObject fileParser;
    private Date createTime;
    private Date updateTime;
    // CDN download URL (absolute path)
    private String cdnUrl;

    public StorageFileInfo() {

    }

    public StorageFileInfo(File file, String relativePath, String fileType, String loadType, String loadDir) {
        this(file, relativePath, fileType);
        this.loadType = loadType;
        this.loadDir = loadDir;
    }

    public StorageFileInfo(File file, String relativePath, String fileType) {
        this.fileType = fileType;
        this.fileName = file.getName();
        this.fileLen = file.length();
        this.blobPath = relativePath + "/" + file.getName();

        try {
            FileInputStream inputStream = new FileInputStream(file);
            this.setMd5(DigestUtils.md5DigestAsHex(inputStream));
            inputStream.close();
        } catch (FileNotFoundException e) {
            throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Generate temp file failed!", e);
        } catch (IOException e) {
            throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Get the MD5 of temp file failed!", e);
        }
    }

    public StorageFileInfo(File file, String relativePath, String fileType, EntityType entityType) {
        this.fileType = fileType;
        this.fileName = file.getName();
        this.fileLen = file.length();
        this.blobPath = relativePath + "/" + file.getName();
        this.blobContainer = entityType.getStorageContainer();

        try {
            FileInputStream inputStream = new FileInputStream(file);
            this.setMd5(DigestUtils.md5DigestAsHex(inputStream));
            inputStream.close();
        } catch (FileNotFoundException e) {
            throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Generate temp file failed!", e);
        } catch (IOException e) {
            throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Get the MD5 of temp file failed!", e);
        }
    }

    public static final class FileType {
        public static final String WINDOWS_APP = "WINAPP";
        public static final String COMMON_FILE = "COMMON";
        public static final String AGENT_PACKAGE = "PACKAGE";
        public static final String APP_FILE = "APP";
        public static final String TEST_APP_FILE = "TEST_APP";
        public static final String T2C_JSON_FILE = "T2C_JSON";
        public static final String SCREENSHOT = "SCREENSHOT";

    }

    public static final class LoadType {
        public static final String COPY = "COPY";
        public static final String UNZIP = "UNZIP";
    }

    public static final class ParserKey {
        public static final String APP_NAME = "appName";
        public static final String PKG_NAME = "pkgName";
        public static final String VERSION = "version";
        public static final String MIN_SDK_VERSION = "minSdkVersion";
        public static final String TARGET_SDK_VERSION = "targetSdkVersion";
    }
}
