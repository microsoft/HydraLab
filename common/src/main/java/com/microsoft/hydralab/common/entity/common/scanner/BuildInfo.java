package com.microsoft.hydralab.common.entity.common.scanner;

import lombok.Data;

@Data
public class BuildInfo {
    private String commitId;
    private String buildFlavor;
    private String buildType;
    private int commitIndex;
}
