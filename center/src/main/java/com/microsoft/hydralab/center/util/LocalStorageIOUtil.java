// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.util;

import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Li Shen
 * @date 3/14/2023
 */

@Slf4j
public final class LocalStorageIOUtil {
    private LocalStorageIOUtil() {
    }

    public static void copyUploadedStreamToFile(InputStream inputStream, String fileUri) {
        File file = resolveFilePath(fileUri).toFile();
        File parentDirFile = new File(file.getParent());
        if (!parentDirFile.exists() && !parentDirFile.mkdirs()) {
            throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "mkdirs failed!");
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            IOUtils.copy(inputStream, fileOutputStream);
        } catch (IOException e) {
            throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "upload file failed!");
        }
    }

    public static Path resolveFilePath(String fileUri) {
        if (fileUri == null) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Invalid file path");
        }
        Path storageRoot = Paths.get(Const.LocalStorageURL.CENTER_LOCAL_STORAGE_ROOT).toAbsolutePath().normalize();
        Path filePath = storageRoot.resolve(fileUri).toAbsolutePath().normalize();
        if (filePath.equals(storageRoot) || !filePath.startsWith(storageRoot)) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Invalid file path");
        }
        return filePath;
    }

    public static int copyDownloadedStreamToResponse(File file, OutputStream os) {
        int resLen;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            resLen = IOUtils.copy(bis, os);
        } catch (IOException e) {
            throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
        return resLen;
    }
}
