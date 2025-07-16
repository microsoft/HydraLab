// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.file.AccessToken;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.util.Const;
import org.apache.commons.lang3.StringUtils;
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
    StorageServiceClientProxy storageServiceClientProxy;
    @Resource
    SysUserService sysUserService;
    @Resource
    UserTeamManagementService userTeamManagementService;
    private final ConcurrentMap<String, AccessToken> accessTokenMap = new ConcurrentHashMap<>();

    public AccessToken generateReadToken(String uniqueId) {
        Assert.notNull(uniqueId, "The key of access token can't be null!");
        AccessToken accessToken = accessTokenMap.get(uniqueId);

        if (accessToken == null || storageServiceClientProxy.isAccessTokenExpired(accessToken)) {
            accessToken = storageServiceClientProxy.generateAccessToken(Const.FilePermission.READ);
            Assert.notNull(accessToken, "Generate access token with READ permission failed! Access token generated is null!");
            accessTokenMap.put(uniqueId, accessToken);
        }

        return accessToken;
    }

    public boolean checkFileAuthorization(SysUser requestor, StorageFileInfo storageFileInfo) {
        if (requestor == null) {
            return false;
        }

        if (storageFileInfo == null) {
            return false;
        }

        // Check if the file is public
        if (storageFileInfo.isPublicFile()) {
            return true;
        }

        // ROLE = SUPER_ADMIN / ADMIN
        if (sysUserService.checkUserAdmin(requestor)) {
            return true;
        }

        // TEAM_ADMIN of current TEAM
        return userTeamManagementService.checkRequestorTeamRelation(requestor, storageFileInfo.getTeamId());
    }

    public AccessToken generateReadTokenForFile(String uniqueId, String fileUri) {
        Assert.notNull(uniqueId, "The key of access token can't be null!");
        Assert.notNull(fileUri, "The file URI can't be null!");
        AccessToken accessToken = accessTokenMap.get(uniqueId + fileUri);

        if (accessToken == null || storageServiceClientProxy.isAccessTokenExpired(accessToken)) {
            accessToken = storageServiceClientProxy.generateAccessTokenForFile(Const.FilePermission.READ, fileUri);
            Assert.notNull(accessToken, "Generate access token with READ permission failed! Access token generated is null!");
            accessTokenMap.put(uniqueId + fileUri, accessToken);
        }

        return accessToken;
    }

    public AccessToken generateWriteToken(String uniqueId) {
        Assert.notNull(uniqueId, "The key of access token can't be null!");
        AccessToken accessToken = accessTokenMap.get(uniqueId);

        if (accessToken == null || storageServiceClientProxy.isAccessTokenExpired(accessToken)) {
            accessToken = storageServiceClientProxy.generateAccessToken(Const.FilePermission.WRITE);
            Assert.notNull(accessToken, "Generate access token with WRITE permission failed! Access token generated is null!");
            accessTokenMap.put(uniqueId, accessToken);
        }

        return accessToken;
    }

    // todo: to be updated when needed: check token in the format of "token=xxx&expiryTime=yyy&permission=zzz"
    public boolean validateAccessToken(String accessToken) {
        return !StringUtils.isBlank(accessToken);
    }

    // todo: specify content
    // for subfield "token" of AccessToken of storage type LOCAL. Differentiate validation method here as AccessToken is split by HTTP PATH EXTRACTION from frontend request already
    public boolean validateTokenVal(String token) {
        return !StringUtils.isBlank(token);
    }

    @Deprecated
    public AccessToken temporaryGetReadSAS(String uniqueId) {
        Assert.notNull(uniqueId, "The key of access token can't be null!");
        AccessToken accessToken = accessTokenMap.get(uniqueId);

        if (accessToken == null || storageServiceClientProxy.isAccessTokenExpired(accessToken)) {
            accessToken = storageServiceClientProxy.generateAccessToken(Const.FilePermission.READ);
            Assert.notNull(accessToken, "Current storage service doesn't config READ permission!");
            accessTokenMap.put(uniqueId, accessToken);
        }

        accessToken.copySignature();
        return accessToken;
    }
}
