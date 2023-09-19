package com.microsoft.hydralab.common.entity.common.scanner;

import lombok.Data;

import java.util.Date;

@Data
public class ApkReport {
    private int id;
    private final Date ingestTime;
    private final String name;

    private final ApkSizeReport apkSizeReport = new ApkSizeReport();
    private final ApkManifest apkManifest = new ApkManifest();
    private final BuildInfo buildInfo = new BuildInfo();

    public ApkReport(String name) {
        ingestTime = new Date();
        this.name = name;
    }
}
