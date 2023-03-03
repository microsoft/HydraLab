// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.file;

import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import lombok.Data;

import java.io.File;

@Data
public abstract class StorageServiceClient {
    protected int fileLimitDay;
    // CDN endpoint
    protected String cdnUrl;

    public abstract void updateAccessToken(AccessToken token);

    public abstract AccessToken generateAccessToken(String permissionType);

    public abstract boolean isAccessTokenExpired(AccessToken token);

    public abstract StorageFileInfo upload(File file, StorageFileInfo fileInfo);

    public abstract StorageFileInfo download(File file, StorageFileInfo fileInfo);
}
