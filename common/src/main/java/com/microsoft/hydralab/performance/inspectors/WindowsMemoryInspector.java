// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.inspectors;

import com.microsoft.hydralab.agent.runner.ITestRun;
import com.microsoft.hydralab.agent.runner.TestRunThreadContext;
import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.common.util.TimeUtils;
import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceInspector;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class WindowsMemoryInspector implements PerformanceInspector {

    // TODO: [Extensible] Make it work with more processes of other Windows apps.
    private static final String PROCESS_NAME_KEYWORD = "Phone";
    private static final String RAW_RESULT_FILE_NAME_FORMAT = "%s_%s_%s";
    private static final String SCRIPT_NAME = "WindowsMemoryInspector.ps1";
    private static final File SCRIPT_FILE = new File(SCRIPT_NAME);
    private static final String PARAMETER_FORMAT = " -keyword %s -output %s";

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
        if (testRun == null) {
            classLogger.error("TestRunThreadContext.getTestRun() return null.");
            return null;
        }

        File rawResultFile = new File(performanceInspection.resultFolder,
                String.format(RAW_RESULT_FILE_NAME_FORMAT, getClass().getSimpleName(), PROCESS_NAME_KEYWORD, TimeUtils.getTimestampForFilename()));
        Process process = ShellUtils.execLocalCommand(
                SCRIPT_FILE.getAbsolutePath() + String.format(PARAMETER_FORMAT, PROCESS_NAME_KEYWORD, rawResultFile),
                false, classLogger);
        PerformanceInspectionResult result = new PerformanceInspectionResult(rawResultFile, performanceInspection);

        try {
            if (process != null && process.waitFor() != 0) {
                classLogger.error("Exit code: " + process.exitValue());
            }
        } catch (InterruptedException e) {
            classLogger.error("InterruptedException caught on process.waitFor().");
            return null;
        }

        return result;
    }

}
