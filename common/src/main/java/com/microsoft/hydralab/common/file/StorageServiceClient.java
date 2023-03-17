// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.file;


import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.io.File;

@Data
public abstract class StorageServiceClient {
    // File in storage may have a TTL to be deleted, so we need to specify a limit day to identify whether we need to reload a file.
    protected int fileLimitDay;
    // CDN endpoint
    protected String cdnUrl;

    public abstract void updateAccessToken(AccessToken token);

    public abstract AccessToken generateAccessToken(String permissionType);

    public abstract boolean isAccessTokenExpired(AccessToken token);

    public abstract StorageFileInfo upload(File file, StorageFileInfo fileInfo);

    public abstract StorageFileInfo download(File file, StorageFileInfo fileInfo);

    public void setFileUrls(StorageFileInfo storageFileInfo, String downloadUrl) {
        storageFileInfo.setBlobUrl(downloadUrl);
        if (StringUtils.isEmpty(this.cdnUrl)) {
            storageFileInfo.setCDNUrl(downloadUrl);
        } else {
            String originDomain = downloadUrl.split("//")[1].split("/")[0];
            storageFileInfo.setCDNUrl(downloadUrl.replace(originDomain, this.cdnUrl));
        }
    }
}
