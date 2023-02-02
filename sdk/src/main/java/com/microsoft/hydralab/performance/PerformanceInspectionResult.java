package com.microsoft.hydralab.performance;

import java.io.File;

public class PerformanceInspectionResult {
    public final long timestamp;
    public File profilingRawResultFile;
    // TODO: restrict the size of it.
    public Object parsedData;
    public String inspector;

    public PerformanceInspectionResult() {
        this.timestamp = System.currentTimeMillis();
    }
}
