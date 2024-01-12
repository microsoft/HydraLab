package com.microsoft.hydralab.common.entity.common.scanner;

import lombok.Data;

import java.io.Serializable;

@Data
public class ApkManifest implements Serializable {
    private String packageName;
    private String versionName;
    private int versionCode;
    private int targetSDKVersion;
    private int minSDKVersion;
}
