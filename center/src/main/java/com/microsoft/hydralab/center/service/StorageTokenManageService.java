// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.common.file.AccessToken;
import com.microsoft.hydralab.common.file.StorageServiceClient;
import com.microsoft.hydralab.common.util.Const;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Li Shen
 * @date 2/21/2023
 */

@Service
public class StorageTokenManageService {
    @Resource
    StorageServiceClient storageServiceClient;
    private final ConcurrentMap<String, AccessToken> accessTokenMap = new ConcurrentHashMap<>();

    public AccessToken generateReadToken(String uniqueId) {
        Assert.notNull(uniqueId, "The key of access token can't be null!");
        AccessToken accessToken = accessTokenMap.get(uniqueId);

        if (accessToken == null || storageServiceClient.isAccessTokenExpired(accessToken)) {
            accessToken = storageServiceClient.generateAccessToken(Const.FilePermission.READ);
            Assert.notNull(accessToken, "Current storage service doesn't config READ permission!");
            accessTokenMap.put(uniqueId, accessToken);
        }

        return accessToken;
    }

    public AccessToken generateWriteToken(String uniqueId) {
        Assert.notNull(uniqueId, "The key of access token can't be null!");
        AccessToken accessToken = accessTokenMap.get(uniqueId);

        if (accessToken == null || storageServiceClient.isAccessTokenExpired(accessToken)) {
            accessToken = storageServiceClient.generateAccessToken(Const.FilePermission.WRITE);
            Assert.notNull(accessToken, "Current storage service doesn't config WRITE permission!");
            accessTokenMap.put(uniqueId, accessToken);
        }

        return accessToken;
    }

    @Deprecated
    public AccessToken temporaryGetReadSAS(String uniqueId) {
        Assert.notNull(uniqueId, "The key of access token can't be null!");
        AccessToken accessToken = accessTokenMap.get(uniqueId);

        if (accessToken == null || storageServiceClient.isAccessTokenExpired(accessToken)) {
            accessToken = storageServiceClient.generateAccessToken(Const.FilePermission.READ);
            Assert.notNull(accessToken, "Current storage service doesn't config READ permission!");
            accessTokenMap.put(uniqueId, accessToken);
        }

        accessToken.copySignature();
        return accessToken;
    }
}
