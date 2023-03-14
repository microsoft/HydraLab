// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.t2c.runner;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class DriverInfo {
    private final String id;
    private final String platform;
    private final String launcherApp;
    private final String initURL;

    public DriverInfo(String id, String platform, String launcherApp, String initURL) {
        this.id = id;
        this.platform = platform;
        this.launcherApp = launcherApp;
        this.initURL = initURL;
    }

    public String getId() {
        return id;
    }

    public String getPlatform() {
        return platform;
    }

    public String getInitURL() {
        return initURL;
    }

    public String getLauncherApp() {
        return launcherApp;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
