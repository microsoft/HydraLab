package com.microsoft.hydralab.common.entity.common.scanner;

import lombok.Data;

@Data
public class ApkManifest {
    private String packageName;
    private String versionName;
    private int versionCode;
    private int targetSDKVersion;
    private int minSDKVersion;
}
