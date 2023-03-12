package com.microsoft.hydralab.agent.environment;

import lombok.Getter;

import java.io.File;

@Getter
public class EnvCapability {

    public enum CapabilityKeyword {
        adb, ffmepg, python, python3, java, node, npm, git, maven, gradle, xcode, appium
    }

    private final String name;
    private final File file;
    private String version;

    public EnvCapability(String name, File file) {
        this.name = name;
        this.file = file;
    }
}
