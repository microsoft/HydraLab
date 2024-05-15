// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.file;

import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.file.impl.azure.AzureBlobClientAdapter;
import com.microsoft.hydralab.common.file.impl.azure.AzureBlobProperty;
import com.microsoft.hydralab.common.file.impl.local.LocalStorageClientAdapter;
import com.microsoft.hydralab.common.file.impl.local.LocalStorageProperty;
import com.microsoft.hydralab.common.util.Const;
import org.springframework.context.ApplicationContext;

import java.io.File;

/**
 * @author Li Shen
 * @date 3/1/2023
 */

public class StorageServiceClientProxy {
    private String storageType;
    private StorageServiceClient storageServiceClient;
    private ApplicationContext applicationContext;

    public StorageServiceClientProxy(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void initCenterStorageClient(String storageType) {
        StorageProperties storageProperties;
        switch (storageType) {
            case Const.StorageType.AZURE:
                storageProperties = applicationContext.getBean(Const.StoragePropertyBean.AZURE, AzureBlobProperty.class);
                storageServiceClient = new AzureBlobClientAdapter(storageProperties);
                break;
            case Const.StorageType.LOCAL:
            default:
                storageProperties = applicationContext.getBean(Const.StoragePropertyBean.LOCAL, LocalStorageProperty.class);
                storageServiceClient = new LocalStorageClientAdapter(storageProperties);
                break;
        }
        EntityType.setInstanceContainer(storageProperties);
    }

    public void initAgentStorageClient(String storageType) {
        StorageProperties storageProperties;
        switch (storageType) {
            case Const.StorageType.AZURE:
                storageServiceClient = new AzureBlobClientAdapter();
                storageProperties = applicationContext.getBean(Const.StoragePropertyBean.AZURE, AzureBlobProperty.class);
                break;
            default:
                storageServiceClient = new LocalStorageClientAdapter();
                storageProperties = applicationContext.getBean(Const.StoragePropertyBean.LOCAL, LocalStorageProperty.class);
                break;
        }
        EntityType.setInstanceContainer(storageProperties);
    }

    public boolean fileExpiryEnabled() {
        return storageServiceClient.getFileExpiryDay() > 0;
    }

    public int getStorageFileExpiryDay() {
        return storageServiceClient.getFileExpiryDay();
    }

    public void updateAccessToken(AccessToken token) {
        storageServiceClient.updateAccessToken(token);
    }

    public AccessToken generateAccessToken(String permissionType) {
        return storageServiceClient.generateAccessToken(permissionType);
    }

    public boolean isAccessTokenExpired(AccessToken token) {
        return storageServiceClient.isAccessTokenExpired(token);
    }

    public StorageFileInfo upload(File file, StorageFileInfo storageFileInfo) {
        return storageServiceClient.upload(file, storageFileInfo);
    }

    public StorageFileInfo download(File file, StorageFileInfo storageFileInfo) {
        return storageServiceClient.download(file, storageFileInfo);
    }
}
