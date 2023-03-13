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
        private boolean ready = false;

        public void setReady(boolean ready) {
            this.ready = ready;
        }

        public boolean isReady() {
            return ready;
        }
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

    public boolean isReady() {
        return keyword.isReady();
    }
}
