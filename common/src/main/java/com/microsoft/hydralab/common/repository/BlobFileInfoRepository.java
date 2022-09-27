// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.repository;

import com.microsoft.hydralab.common.entity.common.BlobFileInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlobFileInfoRepository extends JpaRepository<BlobFileInfo, String> {
    public List<BlobFileInfo> queryBlobFileInfoByMd5(String MD5);
    public List<BlobFileInfo> queryBlobFileInfoByFileType(String fileType);
    public List<BlobFileInfo> queryBlobFileInfoByFileTypeOrderByCreateTimeDesc(String fileType);
}