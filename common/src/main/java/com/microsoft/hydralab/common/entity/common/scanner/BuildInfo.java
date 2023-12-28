package com.microsoft.hydralab.common.entity.common.scanner;

import lombok.Data;

import java.io.Serializable;

@Data
public class BuildInfo implements Serializable {
    private String commitId;
    private String buildFlavor;
    private String buildType;
    private int commitIndex;
}
