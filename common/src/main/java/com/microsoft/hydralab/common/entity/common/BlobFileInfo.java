// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import lombok.Data;
import org.springframework.util.DigestUtils;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

@Entity
@Data
public class BlobFileInfo {
    @Id
    @Column(name = "file_id", nullable = false)
    private String fileId;
    private String fileType;
    private String fileName;
    private String blobUrl;
    private String blobPath;
    private String blobContainer;
    private long fileLen;
    private String md5;
    private String loadDir;
    private String loadType;
    @Convert(converter = JsonConverter.class)
    private JSONObject fileParser;
    private Date createTime;
    private Date updateTime;
    private String CDNUrl;


    public BlobFileInfo() {

    }
    public BlobFileInfo(File file, String relativePath, String fileType,String loadType,String loadDir){
        this(file, relativePath, fileType);
        this.loadType = loadType;
        this.loadDir = loadDir;
    }
    public BlobFileInfo(File file, String relativePath, String fileType) {
        this.fileType = fileType;
        this.fileName = file.getName();
        this.fileLen = file.length();
        this.blobPath = relativePath + "/" + file.getName();

        try {
            FileInputStream inputStream = new FileInputStream(file);
            this.setMd5(DigestUtils.md5DigestAsHex(inputStream));
            inputStream.close();
        } catch (FileNotFoundException e) {
            throw new HydraLabRuntimeException(500, "Generate temp file failed!", e);
        } catch (IOException e) {
            throw new HydraLabRuntimeException(500, "Get the MD5 of temp file failed!", e);
        }
    }

    public interface FileType {
        String WINDOWS_APP = "WINAPP";
        String COMMON_FILE = "COMMON";
        String AGENT_PACKAGE = "PACKAGE";
        String APP_FILE = "APP";
        String TEST_APP_FILE = "TEST_APP";
        String T2C_JSON_FILE = "T2C_JSON";

    }

    public interface LoadType {
        String COPY = "COPY";
        String UNZIP = "UNZIP";
    }

    public interface ParserKey {
        String AppName = "appName";
        String PkgName = "pkgName";
        String Version = "version";
        String MinSdkVersion = "minSdkVersion";
        String TargetSdkVersion = "targetSdkVersion";
    }
}
