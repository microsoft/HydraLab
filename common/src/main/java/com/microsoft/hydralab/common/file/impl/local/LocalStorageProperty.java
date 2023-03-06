// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.file.impl.local;

import com.microsoft.hydralab.common.file.StorageProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Li Shen
 * @date 3/6/2023
 */

@Data
@ConfigurationProperties(prefix = "app.storage.local")
@Component
public class LocalStorageProperty implements StorageProperties {
    public static final List<String> LOCAL_STORAGE_API_PATH_LIST = List.of("/api/storage/local/upload", "/api/storage/local/download");
    private static final String SCREENSHOT_CONTAINER_NAME = "images";
    private static final String APP_FILE_CONTAINER_NAME = "pkgstore";
    private static final String TEST_RESULT_CONTAINER_NAME = "testresults";
    private static final String AGENT_PACKAGE_CONTAINER_NAME = "pkgstore";
    private static final String TEST_JSON_CONTAINER_NAME = "testjson";
    private static final String TEST_SUITE_CONTAINER_NAME = "testsuitestore";
    private String endpoint;
    private String token;

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
