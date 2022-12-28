package com.microsoft.hydralab.performance;

import java.io.File;

public class PerformanceInspectionResult {
    private int type;
    private File profilingRawResultFile;

    public PerformanceInspectionResult(int type, File profilingRawResultFile) {
        this.type = type;
        this.profilingRawResultFile = profilingRawResultFile;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public File getProfilingRawResultFile() {
        return profilingRawResultFile;
    }

    public void setProfilingRawResultFile(File profilingRawResultFile) {
        this.profilingRawResultFile = profilingRawResultFile;
    }
}
