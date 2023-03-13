package com.microsoft.hydralab.agent.environment;

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
        // maven("--version"),
        gradle("--version"),
        // xcode("--version"),
        appium("--version");

        CapabilityKeyword(String fetchVersionParam, String... brokenIndicatorMessageParts) {
            this.fetchVersionParam = fetchVersionParam;
            this.brokenIndicatorMessageParts = brokenIndicatorMessageParts;
        }

        final String fetchVersionParam;
        final String[] brokenIndicatorMessageParts;
        public String versionOutput;
        private int minimumViableMajorVersion;
        private int minimumViableMinorVersion;
        private boolean ready = false;

        public void setReady(boolean ready) {
            this.ready = ready;
        }

        boolean isReady() {
            return ready;
        }
    }

    static {
        CapabilityKeyword.java.minimumViableMajorVersion = 11;
        CapabilityKeyword.python.minimumViableMajorVersion = 3;
        CapabilityKeyword.python.minimumViableMinorVersion = 8;
    }

    private final CapabilityKeyword keyword;
    private final File file;
    private String version;
    private int majorVersion;
    private int minorVersion;

    public EnvCapability(CapabilityKeyword keyword, File file) {
        this.keyword = keyword;
        this.file = file;
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
}
