package com.microsoft.hydralab.performance;

import java.io.File;

public class PerformanceInspectionResult {

    public final long timestamp;
    public PerformanceInspection inspection;

    public File rawResultFile;
    // TODO: restrict the size of it.
    public Object parsedData;

    public PerformanceInspectionResult(File rawResultFile) {
        this.timestamp = System.currentTimeMillis();
        this.rawResultFile = rawResultFile;
    }

    //TODO: overwrite equals, toString, and hashcode methods
}