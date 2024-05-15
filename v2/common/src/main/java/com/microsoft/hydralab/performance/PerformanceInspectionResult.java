// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance;

import org.jetbrains.annotations.Nullable;

import java.io.File;

public class PerformanceInspectionResult {

    @SuppressWarnings("visibilitymodifier")
    public final long timestamp;
    @SuppressWarnings("visibilitymodifier")
    public String testCaseName;
    @SuppressWarnings("visibilitymodifier")
    public PerformanceInspection inspection;
    @SuppressWarnings("visibilitymodifier")
    @Nullable
    public File rawResultFile;
    @SuppressWarnings("visibilitymodifier")
    // TODO: restrict the size of it.
    public Object parsedData;

    public PerformanceInspectionResult(File rawResultFile, PerformanceInspection inspection) {
        this(rawResultFile, inspection, System.currentTimeMillis());
    }

    public PerformanceInspectionResult(File rawResultFile, PerformanceInspection inspection, long timestamp) {
        this.rawResultFile = rawResultFile;
        this.inspection = inspection;
        this.timestamp = timestamp;
    }

    //TODO: overwrite equals, toString, and hashcode methods

    @Override
    public String toString() {
        return "PerformanceInspectionResult{" +
                "timestamp=" + timestamp +
                ", testCaseName='" + testCaseName + '\'' +
                ", inspection=" + inspection +
                ", rawResultFile=" + rawResultFile.getAbsolutePath() +
                ", parsedData=" + parsedData +
                '}';
    }
}