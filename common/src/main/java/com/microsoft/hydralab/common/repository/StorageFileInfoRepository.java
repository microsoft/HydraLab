// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StorageFileInfoRepository extends JpaRepository<StorageFileInfo, String> {
    List<StorageFileInfo> queryStorageFileInfoByMd5(String md5);

    List<StorageFileInfo> queryStorageFileInfoByFileType(String fileType);

    List<StorageFileInfo> queryStorageFileInfoByFileTypeOrderByCreateTimeDesc(String fileType);
}