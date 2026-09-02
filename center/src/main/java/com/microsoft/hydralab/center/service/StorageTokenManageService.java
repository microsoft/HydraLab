// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.file.AccessToken;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.file.impl.local.LocalStorageProperty;
import com.microsoft.hydralab.common.util.Const;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

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
    @Resource
    LocalStorageProperty localStorageProperty;
    private final ConcurrentMap<String, TokenGrant> accessTokenMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TokenGrant> tokenGrantMap = new ConcurrentHashMap<>();

    public AccessToken generateReadToken(String uniqueId) {
        Assert.notNull(uniqueId, "The key of access token can't be null!");
        return getOrGenerateToken(uniqueId, Const.FilePermission.READ, null,
                () -> storageServiceClientProxy.generateAccessToken(Const.FilePermission.READ));
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
        String normalizedFileUri = normalizeFileUri(fileUri);
        return getOrGenerateToken(uniqueId, Const.FilePermission.READ, normalizedFileUri,
                () -> storageServiceClientProxy.generateAccessTokenForFile(Const.FilePermission.READ, normalizedFileUri));
    }

    public AccessToken generateWriteToken(String uniqueId) {
        Assert.notNull(uniqueId, "The key of access token can't be null!");
        return getOrGenerateToken(uniqueId, Const.FilePermission.WRITE, null,
                () -> storageServiceClientProxy.generateAccessToken(Const.FilePermission.WRITE));
    }

    public boolean validateAccessToken(String accessToken, String requiredPermission) {
        return validateAccessToken(accessToken, requiredPermission, null);
    }

    public boolean validateAccessToken(String accessToken, String requiredPermission, String fileUri) {
        if (StringUtils.isBlank(accessToken) || StringUtils.isBlank(requiredPermission)) {
            return false;
        }
        if (tokensEqual(accessToken, localStorageProperty.getToken())) {
            return true;
        }
        return validateGrant(accessToken, requiredPermission, fileUri);
    }

    public boolean validateTokenVal(String token, String fileUri) {
        if (StringUtils.isBlank(token)) {
            return false;
        }
        return validateGrant("token=" + token, Const.FilePermission.READ, fileUri);
    }

    private synchronized AccessToken getOrGenerateToken(String uniqueId, String permission, String fileUri,
                                                        Supplier<AccessToken> tokenSupplier) {
        String key = uniqueId + "|" + permission + "|" + StringUtils.defaultString(fileUri);
        TokenGrant currentGrant = accessTokenMap.get(key);
        if (currentGrant != null && !isGrantExpired(currentGrant)) {
            return currentGrant.accessToken;
        }

        AccessToken accessToken = tokenSupplier.get();
        Assert.notNull(accessToken, "Generate access token with " + permission
                + " permission failed! Access token generated is null!");
        TokenGrant newGrant = new TokenGrant(accessToken, permission, fileUri);
        TokenGrant previousGrant = accessTokenMap.put(key, newGrant);
        if (previousGrant != null) {
            tokenGrantMap.remove(previousGrant.accessToken.getToken(), previousGrant);
        }
        tokenGrantMap.put(accessToken.getToken(), newGrant);
        return accessToken;
    }

    private boolean validateGrant(String token, String requiredPermission, String fileUri) {
        TokenGrant grant = tokenGrantMap.get(token);
        if (grant == null || !requiredPermission.equals(grant.permission)) {
            return false;
        }
        if (isGrantExpired(grant)) {
            tokenGrantMap.remove(token, grant);
            return false;
        }
        if (grant.fileUri == null) {
            return true;
        }
        try {
            return grant.fileUri.equals(normalizeFileUri(fileUri));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isGrantExpired(TokenGrant grant) {
        return !Const.FilePermission.WRITE.equals(grant.permission)
                && storageServiceClientProxy.isAccessTokenExpired(grant.accessToken);
    }

    private static String normalizeFileUri(String fileUri) {
        if (StringUtils.isBlank(fileUri)) {
            throw new IllegalArgumentException("The file URI can't be blank");
        }
        try {
            return Paths.get(fileUri).normalize().toString().replace(File.separatorChar, '/');
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid file URI", e);
        }
    }

    private static boolean tokensEqual(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        return MessageDigest.isEqual(first.getBytes(StandardCharsets.UTF_8), second.getBytes(StandardCharsets.UTF_8));
    }

    private static final class TokenGrant {
        private final AccessToken accessToken;
        private final String permission;
        private final String fileUri;

        private TokenGrant(AccessToken accessToken, String permission, String fileUri) {
            this.accessToken = accessToken;
            this.permission = permission;
            this.fileUri = fileUri;
        }
    }

    @Deprecated
    public AccessToken temporaryGetReadSAS(String uniqueId) {
        Assert.notNull(uniqueId, "The key of access token can't be null!");
        AccessToken accessToken = generateReadToken(uniqueId);
        accessToken.copySignature();
        return accessToken;
    }
}
