// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance.inspectors;

import com.microsoft.hydralab.agent.runner.ITestRun;
import com.microsoft.hydralab.agent.runner.TestRunThreadContext;
import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceInspector;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class WindowsMemoryInspector implements PerformanceInspector {

    private final static String PROCESS_NAME_KEYWORD = "Phone";
    private final static String RAW_RESULT_FILE_NAME_FORMAT = "%s_%s_%s";
    private final static String SCRIPT_NAME = "WindowsMemoryInspector.ps1";
    private final static File SCRIPT_FILE = new File(SCRIPT_NAME);
    private final static String PARAMETER_FORMAT = " -keyword %s -output %s";

    protected Logger classLogger = LoggerFactory.getLogger(getClass());

    public void initializeIfNeeded(PerformanceInspection performanceInspection) {
        if (!SCRIPT_FILE.exists()) {
            try {
                InputStream resourceAsStream = FileUtils.class.getClassLoader().getResourceAsStream(SCRIPT_NAME);
                OutputStream out = new FileOutputStream(SCRIPT_FILE);
                IOUtils.copy(Objects.requireNonNull(resourceAsStream), out);
                out.close();
            } catch (IOException e) {
                classLogger.error("Failed to find app handler script", e);
            }
        }
    }

    @Override
    public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
        initializeIfNeeded(performanceInspection);

        ITestRun testRun = TestRunThreadContext.getTestRun();
        if (testRun == null)
        {
            classLogger.error("TestRunThreadContext.getTestRun() return null.");
            return null;
        }

        File rawResultFile = new File(performanceInspection.resultFolder,
                String.format(RAW_RESULT_FILE_NAME_FORMAT, getClass().getSimpleName(), PROCESS_NAME_KEYWORD, getTimestamp()));
        Process process = ShellUtils.execLocalCommand(
                SCRIPT_FILE.getAbsolutePath() + String.format(PARAMETER_FORMAT, PROCESS_NAME_KEYWORD, rawResultFile),
                false, classLogger);
        PerformanceInspectionResult result = new PerformanceInspectionResult(rawResultFile);

        try {
            if (process != null && process.waitFor() != 0)
            {
                classLogger.error("Exit code: " + process.exitValue());
            }
        } catch (InterruptedException e) {
            classLogger.error("InterruptedException caught on process.waitFor().");
            return null;
        }

        return result;
    }

    private String getTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH_mm_ss",
                Locale.getDefault());
        TimeZone gmt = TimeZone.getTimeZone("UTC");
        dateFormat.setTimeZone(gmt);
        dateFormat.setLenient(true);
        return dateFormat.format(new Date());
    }

}
