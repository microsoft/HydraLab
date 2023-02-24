// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.entity;

import com.google.gson.JsonObject;
import java.util.Date;

public class StorageFileInfo {
    public String fileId;
    public String fileType;
    public String fileName;

    public String fileDownloadUrl;
    public String storageContainer;
    public String fileRelPath;

    public long fileLen;
    public String md5;
    public String loadDir;
    public String loadType;
    public Date createTime;
    public Date updateTime;
    public String CDNUrl;

}