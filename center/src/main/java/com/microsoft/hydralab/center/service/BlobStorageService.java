// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.common.entity.common.SASData;
import com.microsoft.hydralab.common.util.blob.BlobStorageClient;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author zhoule
 * @date 10/24/2022
 */

@Service
public class BlobStorageService {
    @Resource
    BlobStorageClient blobStorageClient;
    private ConcurrentMap<String, SASData> sasMap = new ConcurrentHashMap<>();

    public SASData generateReadSAS(String uniqueId) {
        Assert.notNull(uniqueId, "The key of SAS can't be null!");
        SASData sasData = sasMap.get(uniqueId);

        if (sasData == null || blobStorageClient.isSASExpired(sasData)) {
            sasData = blobStorageClient.generateSAS(SASData.SASPermission.Read);
            sasMap.put(uniqueId, sasData);
        }

        return sasData;
    }

    public SASData generateWriteSAS(String uniqueId) {
        Assert.notNull(uniqueId, "The key of SAS can't be null!");
        SASData sasData = sasMap.get(uniqueId);

        if (sasData == null || blobStorageClient.isSASExpired(sasData)) {
            sasData = blobStorageClient.generateSAS(SASData.SASPermission.Write);
            sasMap.put(uniqueId, sasData);
        }

        return sasData;
    }
}
