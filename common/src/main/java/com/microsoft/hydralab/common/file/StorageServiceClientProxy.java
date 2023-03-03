// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.file;

import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.file.impl.azure.AzureBlobClientAdapter;
import com.microsoft.hydralab.common.file.impl.azure.AzureBlobProperty;
import com.microsoft.hydralab.common.util.Const;
import org.springframework.context.ApplicationContext;

import java.io.File;

/**
 * @author Li Shen
 * @date 3/1/2023
 */

public class StorageServiceClientProxy extends StorageServiceClient {
    private StorageServiceClient storageServiceClient;
    private ApplicationContext applicationContext;

    public StorageServiceClientProxy(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void initCenterStorageClient(String storageType) {
        switch (storageType) {
            case Const.StorageType.AZURE:
                AzureBlobProperty azureBlobProperty = applicationContext.getBean(Const.StoragePropertyBean.AZURE, AzureBlobProperty.class);
                storageServiceClient = new AzureBlobClientAdapter(azureBlobProperty);
                EntityType.setInstanceContainer(azureBlobProperty);
                break;
            default:
                // todo: local storage system
                break;
        }
    }

    public void initAgentStorageClient(String storageType) {
        switch (storageType) {
            case Const.StorageType.AZURE:
                storageServiceClient = new AzureBlobClientAdapter();
                AzureBlobProperty azureBlobProperty = applicationContext.getBean(Const.StoragePropertyBean.AZURE, AzureBlobProperty.class);
                EntityType.setInstanceContainer(azureBlobProperty);
                break;
            default:
                // todo: local storage system
                break;
        }
    }

    public int getStorageFileLimitDay() {
        return storageServiceClient.getFileLimitDay();
    }

    @Override
    public void updateAccessToken(AccessToken token) {
        storageServiceClient.updateAccessToken(token);
    }

    @Override
    public AccessToken generateAccessToken(String permissionType) {
        return storageServiceClient.generateAccessToken(permissionType);
    }

    @Override
    public boolean isAccessTokenExpired(AccessToken token) {
        return storageServiceClient.isAccessTokenExpired(token);
    }

    @Override
    public StorageFileInfo upload(File file, StorageFileInfo storageFileInfo) {
        return storageServiceClient.upload(file, storageFileInfo);
    }

    @Override
    public StorageFileInfo download(File file, StorageFileInfo storageFileInfo) {
        return storageServiceClient.download(file, storageFileInfo);
    }
}
