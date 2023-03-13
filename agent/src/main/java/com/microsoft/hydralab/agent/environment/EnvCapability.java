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
