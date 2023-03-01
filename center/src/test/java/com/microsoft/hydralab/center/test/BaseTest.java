// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.test;

import com.microsoft.hydralab.center.util.AuthUtil;
import com.microsoft.hydralab.common.util.StorageManageService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.transaction.Transactional;

/**
 * @author zhoule
 * @date 11/10/2022
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@EnableCaching
@Transactional
@Rollback
@Disabled
public class BaseTest {
    protected Logger baseLogger = LoggerFactory.getLogger(BaseTest.class);
    @MockBean
    StorageManageService storageManageService;
    @MockBean
    AuthUtil authUtil;
}
