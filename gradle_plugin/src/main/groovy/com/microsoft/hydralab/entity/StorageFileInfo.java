// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.entity;

import com.google.gson.JsonObject;
import java.util.Date;

public class StorageFileInfo {
    public String fileId;
    public String fileType;
    public String fileName;
    public String blobUrl;
    public String blobPath;
    public long fileLen;
    public String md5;
    public String loadDir;
    public String loadType;
    public JsonObject fileParser;
    public Date createTime;
    public Date updateTime;


    public interface fileType {
        String WINDOWS_APP = "WINAPP";
        String COMMON_FILE = "COMMON";
        String AGENT_PACKAGE = "PACKAGE";
        String APP_FILE = "APP";
        String TEST_APP_FILE = "TEST_APP";
    }

    public interface loadType {
        String COPY = "COPY";
        String UNZIP = "UNZIP";
    }
}