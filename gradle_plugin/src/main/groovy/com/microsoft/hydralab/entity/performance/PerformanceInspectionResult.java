// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.entity.performance;

import java.io.File;

public class PerformanceInspectionResult {

    @SuppressWarnings("visibilitymodifier")
    public final long timestamp;
    @SuppressWarnings("visibilitymodifier")
    public PerformanceInspection inspection;
    @SuppressWarnings("visibilitymodifier")
    public File rawResultFile;
    @SuppressWarnings("visibilitymodifier")
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