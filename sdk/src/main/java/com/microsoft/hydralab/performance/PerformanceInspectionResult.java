package com.microsoft.hydralab.performance;

import java.io.File;

public class PerformanceInspectionResult {
    public final long timestamp;
    public PerformanceInspection inspection;

    public File profilingRawResultFile;
    // TODO: restrict the size of it.
    public Object parsedData;

    public PerformanceInspectionResult() {
        this.timestamp = System.currentTimeMillis();
    }
}
