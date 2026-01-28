// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.agent;

import lombok.Getter;

import java.io.File;

@Getter
public class EnvCapability {

    @SuppressWarnings("checkstyle:VisibilityModifier")
    public enum CapabilityKeyword {
        adb("--version"),
        ffmpeg("-version"),
        python("--version"),
        python3("--version"),
        // it's java -version on JDK7, java --version on JDK11 ...
        // -version is more common, so we use it, but the output is on stderr ...
        // https://www.java.com/en/download/help/version_manual.html
        // And Java version is a bit tricky as some are 11.0.1 for JDK 11, some are 1.7.0 for JDK 7
        // So the major version is the first OR second part of the version string
        java("-version", "Unable to locate a Java Runtime"),
        node("--version"),
        npm("--version"),
        git("--version"),
        tidevice("-v"),
        pymobiledevice3("-h"),  // pymobiledevice3 uses -h or --help instead of --version
        // maven("--version"),
        gradle("--version"),
        // xcode("--version"),
        maestro("--version"),
        appium("--version"),
        apkleaks("--help"),
        apkanalyzer("-h");

        CapabilityKeyword(String fetchVersionParam, String... brokenIndicatorMessageParts) {
            this.fetchVersionParam = fetchVersionParam;
            this.brokenIndicatorMessageParts = brokenIndicatorMessageParts;
        }

        final String fetchVersionParam;
        final String[] brokenIndicatorMessageParts;
        private String versionOutput;
        private int minimumViableMajorVersion;
        private int minimumViableMinorVersion;
        private boolean ready = false;

        private void setReady(boolean ready) {
            this.ready = ready;
        }

        boolean isReady() {
            return ready;
        }

        public void setVersionOutput(String versionOutput) {
            this.versionOutput = versionOutput;
            determineCapReadyFromBrokenMessage();
        }

        private void determineCapReadyFromBrokenMessage() {
            setReady(true);
            if (brokenIndicatorMessageParts == null) {
                return;
            }
            for (String message : brokenIndicatorMessageParts) {
                if (!versionOutput.contains(message)) {
                    continue;
                }
                setReady(false);
            }
        }

        public String getVersionOutput() {
            return versionOutput;
        }

        public String getFetchVersionParam() {
            return fetchVersionParam;
        }
    }

    static {
        CapabilityKeyword.java.minimumViableMajorVersion = 11;
        CapabilityKeyword.python.minimumViableMajorVersion = 3;
        CapabilityKeyword.python.minimumViableMinorVersion = 8;
    }

    private final CapabilityKeyword keyword;
    private transient final File file;
    private String version;
    private int majorVersion;
    private int minorVersion;

    public EnvCapability(CapabilityKeyword keyword, File file) {
        this.keyword = keyword;
        this.file = file;
    }

    public EnvCapability(CapabilityKeyword keyword, int majorVersion, int minorVersion) {
        this.keyword = keyword;
        this.file = null;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public void setVersion(String version) {
        this.version = version;
        String[] versionParts = version.split("\\.");
        majorVersion = Integer.parseInt(versionParts[0]);
        minorVersion = Integer.parseInt(versionParts[1]);
        handleJavaVersion(versionParts);
    }

    private void handleJavaVersion(String[] versionParts) {
        // there is no chance it's a JDK1, so it must be something like a 1.7.0
        if (keyword == CapabilityKeyword.java && majorVersion == 1) {
            majorVersion = minorVersion;
            if (versionParts.length > 2) {
                minorVersion = Integer.parseInt(versionParts[2]);
            } else {
                minorVersion = 0;
            }
        }
    }

    public boolean isReady() {
        if (!keyword.isReady()) {
            return false;
        }
        if (keyword.minimumViableMajorVersion > 0) {
            if (majorVersion < keyword.minimumViableMajorVersion) {
                return false;
            }
            if (majorVersion == keyword.minimumViableMajorVersion) {
                return minorVersion >= keyword.minimumViableMinorVersion;
            }
        }
        return true;
    }

    public boolean meet(EnvCapability envCapabilityRequirement) {
        if (envCapabilityRequirement == null || keyword != envCapabilityRequirement.keyword || !isReady()) {
            return false;
        }

        if (envCapabilityRequirement.majorVersion < majorVersion) {
            return true;
        } else if (envCapabilityRequirement.majorVersion == majorVersion) {
            return envCapabilityRequirement.minorVersion <= minorVersion;
        }

        return false;
    }
}
