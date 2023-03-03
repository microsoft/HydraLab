package com.microsoft.hydralab.common.file;

public interface StorageProperties {
    String getScreenshotContainerName();
    String getAppFileContainerName();
    String getTestResultContainerName();
    String getAgentPackageContainerName();
    String getTestJsonContainerName();
    String getTestSuiteContainerName();
}
