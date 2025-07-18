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
    private String CDNUrl;
    @Column(name = "team_id")
    private String teamId;
    private String teamName;


    public StorageFileInfo() {

    }

    public StorageFileInfo(File file, String relativeParent, String fileType, String loadType, String loadDir, String teamId, String teamName) {
        this(file, relativeParent, fileType, teamId, teamName);
        this.loadType = loadType;
        this.loadDir = loadDir;
    }

    public StorageFileInfo(File file, String relativeParent, String fileType, String teamId, String teamName) {
        this.fileType = fileType;
        this.fileName = file.getName();
        this.fileLen = file.length();
        this.blobPath = relativeParent + "/" + file.getName();
        this.teamId = teamId;
        this.teamName = teamName;

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

    public StorageFileInfo(File file, String fileRelPath, String fileType, EntityType entityType, String teamId, String teamName) {
        this.fileType = fileType;
        this.fileName = file.getName();
        this.fileLen = file.length();
        this.blobPath = fileRelPath;
        this.blobContainer = entityType.storageContainer;
        this.teamId = teamId;
        this.teamName = teamName;

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

    public boolean isPublicFile() {
        if (teamId == null || teamId.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    public interface FileType {
        String WINDOWS_APP = "WINAPP";
        String COMMON_FILE = "COMMON";
        String AGENT_PACKAGE = "PACKAGE";
        String APP_FILE = "APP";
        String TEST_APP_FILE = "TEST_APP";
        String T2C_JSON_FILE = "T2C_JSON";
        String SCREENSHOT = "SCREENSHOT";

    }

    public interface LoadType {
        String COPY = "COPY";
        String UNZIP = "UNZIP";
    }

    public interface ParserKey {
        String APP_NAME = "appName";
        String PKG_NAME = "pkgName";
        String VERSION = "version";
        String MIN_SDK_VERSION = "minSdkVersion";
        String TARGET_SDK_VERSION = "targetSdkVersion";
    }
}
