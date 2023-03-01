// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import java.io.File;

public class PerformanceInspectionResult {

    public final long timestamp;
    public PerformanceInspection inspection;

    public File rawResultFile;
    // TODO: restrict the size of it.
    public Object parsedData;

    public PerformanceInspectionResult(File rawResultFile, PerformanceInspection inspection) {
        this.timestamp = System.currentTimeMillis();
        this.rawResultFile = rawResultFile;
        this.inspection = inspection;
    }

    //TODO: overwrite equals, toString, and hashcode methods

    @Override
    public String toString() {
        return "PerformanceInspectionResult{" +
                "timestamp=" + timestamp +
                ", inspection=" + inspection +
                ", rawResultFile=" + rawResultFile.getAbsolutePath() +
                ", parsedData=" + parsedData +
                '}';
    }
}