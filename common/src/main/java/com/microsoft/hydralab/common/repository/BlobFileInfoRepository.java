// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlobFileInfoRepository extends JpaRepository<StorageFileInfo, String> {
    List<StorageFileInfo> queryBlobFileInfoByMd5(String MD5);
    List<StorageFileInfo> queryBlobFileInfoByFileType(String fileType);
    List<StorageFileInfo> queryBlobFileInfoByFileTypeOrderByCreateTimeDesc(String fileType);
}