// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.file.local;

import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import javax.transaction.Transactional;

/**
 * @author Li Shen
 * @date 3/14/2023
 */

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("testLocal")
@EnableCaching
@Transactional
@Rollback
public class LocalStorageServiceTest {
    @Resource
    ApplicationContext applicationContext;
    @Value("${app.storage.type}")
    String storageType;

    @Test
    public void initCenterStorageService() {
        StorageServiceClientProxy storageServiceClientProxy = new StorageServiceClientProxy(applicationContext);
        storageServiceClientProxy.initCenterStorageClient(storageType);
    }
}
