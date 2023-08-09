// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author : shbu
 * @version : 1.0
 * @date : 2019/01/18
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppOptions {

    private String location;

    public String getLocation() {
        return location;
    }

    public static final String DEVICE_STORAGE_MAPPING_REL_PATH = "/devices";

    public String getDeviceStorageLocation() {
        return location + "/storage/devices/";
    }

    public String getPreAppStorageLocation() {
        return location + "/storage/preApp";
    }

    public String getDeviceLogStorageLocation() {
        return getDeviceStorageLocation() + "/log/";
    }

    public String getScreenshotStorageLocation() {
        return getDeviceStorageLocation() + "/screenshot/";
    }

    public String getTestPackageLocation() {
        return location + "/storage/test/package/";
    }

    public static final String TEST_CASE_RESULT_STORAGE_MAPPING_REL_PATH = "/test/result";

    public String getTestCaseResultLocation() {
        return location + "/storage/test/result/";
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getErrorStorageLocation() {
        return location + "/storage/errorOutput/";
    }
}
