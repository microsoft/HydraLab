// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.test.BaseTest;
import com.microsoft.hydralab.common.entity.common.TestTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import javax.annotation.Resource;

/**
 * @author zhoule
 * @date 12/29/2022
 */

public class CacheServiceTest extends BaseTest {
    @Resource
    CacheManager cacheManager;

    @Test
    void putData() {
        TestTask taskA = new TestTask();
        Cache container = cacheManager.getCache("taskCache");
        container.put(taskA.getId(), taskA);
        TestTask taskB = (TestTask) container.get(taskA.getId()).get();
        Assertions.assertEquals(taskA.getId(), taskB.getId(), "Put data into cache error!");
    }
}
