// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.file.local;

import com.microsoft.hydralab.center.test.BaseTest;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;

/**
 * @author Li Shen
 * @date 3/14/2023
 */

public class LocalStorageServiceTest extends BaseTest {
    @Resource
    ApplicationContext applicationContext;

    @Test
    public void initLocalCenterStorageService() {
        StorageServiceClientProxy storageServiceClientProxy = new StorageServiceClientProxy(applicationContext);
        storageServiceClientProxy.initCenterStorageClient("LOCAL");
    }
}
