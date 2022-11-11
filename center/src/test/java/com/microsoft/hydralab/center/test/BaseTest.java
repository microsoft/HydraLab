// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.test;

import com.microsoft.hydralab.center.util.AuthUtil;
import com.microsoft.hydralab.common.util.blob.BlobStorageClient;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;

/**
 * @author zhoule
 * @date 11/10/2022
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
@Rollback
public class BaseTest {
    protected Logger baseLogger = LoggerFactory.getLogger(BaseTest.class);
    @MockBean
    BlobStorageClient blobStorageClient;
    @MockBean
    AuthUtil authUtil;
}
