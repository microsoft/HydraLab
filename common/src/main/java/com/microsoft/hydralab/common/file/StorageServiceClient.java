// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.file;


import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.io.File;

@Data
public abstract class StorageServiceClient {
    /**
     * File in storage may have a time limit to be deleted, so a time limit is necessary to be set in Hydra Lab to verify whether we need to re-upload a file to make sure the existence.
     * Normally, this value is slightly smaller than the limit setting in third-party storage setting.
     */
    protected int fileExpiryDay;
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

    public abstract AccessToken generateAccessTokenForFile(String permissionType, String fileUri);
}
