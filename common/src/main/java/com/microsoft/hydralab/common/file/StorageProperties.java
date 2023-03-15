package com.microsoft.hydralab.common.file;

public abstract class StorageProperties {
    protected static final String SCREENSHOT_CONTAINER_NAME = "images";
    protected static final String APP_FILE_CONTAINER_NAME = "pkgstore";
    protected static final String TEST_RESULT_CONTAINER_NAME = "testresults";
    protected static final String AGENT_PACKAGE_CONTAINER_NAME = "pkgstore";
    protected static final String TEST_JSON_CONTAINER_NAME = "testjson";
    protected static final String TEST_SUITE_CONTAINER_NAME = "testsuitestore";

    public String getScreenshotContainerName() {
        return SCREENSHOT_CONTAINER_NAME;
    }

    public String getAppFileContainerName() {
        return APP_FILE_CONTAINER_NAME;
    }

    public String getTestResultContainerName() {
        return TEST_RESULT_CONTAINER_NAME;
    }

    public String getAgentPackageContainerName() {
        return AGENT_PACKAGE_CONTAINER_NAME;
    }

    public String getTestJsonContainerName() {
        return TEST_JSON_CONTAINER_NAME;
    }

    public String getTestSuiteContainerName() {
        return TEST_SUITE_CONTAINER_NAME;
    }
}
