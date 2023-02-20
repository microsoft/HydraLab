package com.microsoft.hydralab.common.file;

import java.io.File;

public interface FileServiceClientAdapter {
    void updateToken(AccessToken accessToken);
    AccessToken generateToken(PermissionConfiguration permissionConfiguration);
    boolean isTokenExpired(AccessToken accessToken);
    SyncResult upload(File file, ResourceLocation location);
    SyncResult download(File downloadToFile, ResourceLocation location);
}
