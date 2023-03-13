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
        java("-version"),
        node("--version"),
        npm("--version"),
        git("--version"),
        // maven("--version"),
        gradle("--version"),
        // xcode("--version"),
        appium("--version");

        CapabilityKeyword(String fetchVersionParam) {
            this.fetchVersionParam = fetchVersionParam;
        }

        final String fetchVersionParam;
        public String versionOutput;
    }

    private final CapabilityKeyword keyword;
    private final File file;
    private String version;

    public EnvCapability(CapabilityKeyword keyword, File file) {
        this.keyword = keyword;
        this.file = file;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
