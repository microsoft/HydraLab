// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.file.impl.azure;

import com.microsoft.hydralab.common.file.StorageProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author zhoule
 * @date 11/15/2022
 */
@Data
@ConfigurationProperties(prefix = "app.storage.blob")
@Component
public class AzureBlobProperty implements StorageProperties {
    private String connection;
    private long SASExpiryTimeFront;
    private long SASExpiryTimeAgent;
    private long SASExpiryUpdate;
    private String timeUnit;
    private int fileLimitDay;
    private String CDNUrl;

    private static final String SCREENSHOT_CONTAINER_NAME = "images";
    private static final String APP_FILE_CONTAINER_NAME = "pkgstore";
    private static final String TEST_RESULT_CONTAINER_NAME = "testresults";
    private static final String AGENT_PACKAGE_CONTAINER_NAME = "pkgstore";
    private static final String TEST_JSON_CONTAINER_NAME = "testjson";
    private static final String TEST_SUITE_CONTAINER_NAME = "testsuitestore";

    @Override
    public String getScreenshotContainerName() {
        return SCREENSHOT_CONTAINER_NAME;
    }

    @Override
    public String getAppFileContainerName() {
        return APP_FILE_CONTAINER_NAME;
    }

    @Override
    public String getTestResultContainerName() {
        return TEST_RESULT_CONTAINER_NAME;
    }

    @Override
    public String getAgentPackageContainerName() {
        return AGENT_PACKAGE_CONTAINER_NAME;
    }

    @Override
    public String getTestJsonContainerName() {
        return TEST_JSON_CONTAINER_NAME;
    }

    @Override
    public String getTestSuiteContainerName() {
        return TEST_SUITE_CONTAINER_NAME;
    }
}
