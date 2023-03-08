// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.entity.performance;

import java.io.File;

public class PerformanceInspectionResult {
    public final long timestamp;
    public PerformanceInspection inspection;
    public File rawResultFile;
    public Object parsedData;

    public PerformanceInspectionResult(File rawResultFile, PerformanceInspection inspection) {
        this.timestamp = System.currentTimeMillis();
        this.rawResultFile = rawResultFile;
        this.inspection = inspection;
    }

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